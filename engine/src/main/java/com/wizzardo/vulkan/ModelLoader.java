package com.wizzardo.vulkan;

import com.wizzardo.vulkan.material.predefined.UnshadedColor;
import com.wizzardo.vulkan.material.predefined.UnshadedTexture;
import com.wizzardo.vulkan.misc.IntArrayBuilder;
import com.wizzardo.vulkan.scene.Geometry;
import com.wizzardo.vulkan.scene.Node;
import com.wizzardo.vulkan.scene.Spatial;
import org.joml.*;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;
import org.lwjgl.system.Configuration;

import java.io.File;
import java.io.IOException;
import java.lang.Math;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static org.lwjgl.assimp.Assimp.*;
import static org.lwjgl.system.MemoryUtil.*;

public class ModelLoader {


    static {
        Configuration.DISABLE_FUNCTION_CHECKS.set(true);
        Assimp.aiGetVersionMajor();
        System.out.println("Assimp.aiGetVersionMajor() " + Assimp.aiGetVersionMajor());
        Configuration.DISABLE_FUNCTION_CHECKS.set(false);
//        for (int i = 0; i < aiGetImportFormatCount(); i++) {
//            AIImporterDesc importerDesc = aiGetImportFormatDescription(i);
//            System.out.println(importerDesc.mNameString()+": "+importerDesc.mFileExtensionsString());
//        }
        AILogStream logStream = AILogStream.create();
        logStream.callback((message, user) -> {
            String s = memUTF8(message);
            System.out.println("assimp: " + s);
        });
        Assimp.aiAttachLogStream(logStream);
    }

    public static Node loadModel(File file, int flags) {
        try (AIScene scene = aiImportFile(file.getAbsolutePath(), flags)) {
            if (scene == null || scene.mRootNode() == null) {
                throw new RuntimeException("Could not load model: " + aiGetErrorString());
            }

            Node s = new Node();
            List<AssimpMaterial> materials = processMaterials(scene);
            processNode(scene.mRootNode(), scene, s, convertMaterials(materials));
            return s;
        }
    }

    public interface AssetLoader {
        ByteBuffer load(String asset) throws IOException;
    }

    public static Node loadModel(ByteBuffer file, int flags, String hint) {
        try (AIScene scene = aiImportFileFromMemory(file, flags, hint)) {
            if (scene == null || scene.mRootNode() == null) {
                throw new RuntimeException("Could not load model: " + aiGetErrorString());
            }

            Node s = new Node();
            List<AssimpMaterial> materials = processMaterials(scene);
            processNode(scene.mRootNode(), scene, s, convertMaterials(materials));
            return s;
        }
    }

    public static Node loadModel(String path, int flags, AssetLoader assetLoader) {
        AIFileIO fileIO = AIFileIO.create();

        try {
            Map<String, ByteBuffer> loaded = new HashMap<>();
            Map<Long, AIFile> openFiles = new HashMap<>();
            fileIO.OpenProc((pFileIO, fileName, openMode) -> {
                        ByteBuffer data;
                        String fileNameUtf8 = memUTF8(fileName);
                        try {
                            ByteBuffer byteBuffer = loaded.get(fileNameUtf8);
                            if (byteBuffer != null) {
                                data = byteBuffer.slice();
                            } else {
                                System.out.println("trying to open file " + fileNameUtf8);
                                data = assetLoader.load(fileNameUtf8);
                                loaded.put(fileNameUtf8, data.slice());
                            }
                        } catch (IOException e) {
                            throw new RuntimeException("Could not open file: " + fileNameUtf8);
                        }

                        AIFile file = AIFile.create();
                        openFiles.put(file.address(), file);
                        return file
                                .ReadProc((pFile, pBuffer, size, count) -> {
                                    long max = Math.min(data.remaining(), size * count);
                                    if (max != size * count && max % size != 0) {
                                        max -= max % size;
                                    }
                                    System.out.println(fileNameUtf8 + ".read " + max + "  " + size + "  " + count);
                                    if (data.position() != 0 || max != 200 || fileNameUtf8.toLowerCase().endsWith("obj")) {
                                        memCopy(memAddress(data) + data.position(), pBuffer, max);
                                    }
                                    data.position((int) (data.position() + max));
                                    return max / size;
                                })
                                .TellProc(pFile -> data.position())
                                .SeekProc((pFile, offset, origin) -> {
                                    if (origin == aiOrigin_CUR) {
                                        data.position(data.position() + (int) offset);
                                    } else if (origin == aiOrigin_SET) {
                                        data.position((int) offset);
                                    } else if (origin == aiOrigin_END) {
                                        data.position(data.limit() + (int) offset);
                                    }
                                    System.out.println(fileNameUtf8 + ".seek " + data.position() + " " + origin);
                                    return 0;
                                })
                                .FileSizeProc(pFile -> data.limit())
                                .address();
                    })
                    .CloseProc((pFileIO, pFile) -> {
                        AIFile aiFile = openFiles.get(pFile);
                        if (aiFile == null)
                            return;

                        aiFile.ReadProc().free();
                        aiFile.SeekProc().free();
                        aiFile.FileSizeProc().free();
                        aiFile.TellProc().free();

                        openFiles.remove(pFile);
                    });

            try (AIScene scene = aiImportFileEx(path, flags, fileIO)) {
                if (scene == null || scene.mRootNode() == null) {
                    throw new RuntimeException("Could not load model: " + aiGetErrorString());
                }

                String folder = path.substring(0, path.lastIndexOf('/'));
                Node s = new Node();
                List<AssimpMaterial> assimpMaterials = processMaterials(scene);
                List<Material> materials = convertMaterials(assimpMaterials);
                for (Material material : materials) {
                    for (TextureImage texture : material.getTextures()) {
                        texture.setFilename(folder + "/" + texture.filename);
                    }
                }
                processNode(scene.mRootNode(), scene, s, materials);
                return s;
            }

        } finally {
            fileIO.OpenProc().free();
            fileIO.CloseProc().free();
        }
    }

    protected static List<Material> convertMaterials(List<AssimpMaterial> assimpMaterials) {
        return assimpMaterials.stream().map(assimpMaterial -> {
            if (!assimpMaterial.textures.isEmpty()) {
                assimpMaterial.textures.sort(Comparator.comparingInt(o -> o.type.ordinal()));
                UnshadedTexture material = new UnshadedTexture();
                for (TextureInfo texture : assimpMaterial.textures) {
                    material.addTextureImage(new TextureImage(texture.fileName, texture.type.toTextureImageType()));
                }
                return material;
            }
            return new UnshadedColor(new Vector3f(assimpMaterial.diffuseColor.x, assimpMaterial.diffuseColor.y, assimpMaterial.diffuseColor.z));
        }).collect(Collectors.toList());
    }

    private static void processNode(AINode node, AIScene scene, Node s, List<Material> materials) {
        s.setName(node.mName().dataString());
        AIMatrix4x4 transformation = node.mTransformation();
        Matrix4f matrix4f = new Matrix4f(
                transformation.a1(), transformation.b1(), transformation.c1(), transformation.d1(),
                transformation.a2(), transformation.b2(), transformation.c2(), transformation.d2(),
                transformation.a3(), transformation.b3(), transformation.c3(), transformation.d3(),
                transformation.a4(), transformation.b4(), transformation.c4(), transformation.d4()
        );
        matrix4f.getTranslation(s.getLocalTransform().getTranslation());
        matrix4f.getScale(s.getLocalTransform().getScale());
        matrix4f.getUnnormalizedRotation(s.getLocalTransform().getRotation());

        if (node.mNumMeshes() != 0) {
            processNodeMeshes(scene, node, s, materials);
        }

        if (node.mChildren() != null) {
            PointerBuffer children = node.mChildren();
            for (int i = 0; i < node.mNumChildren(); i++) {
                AINode child = AINode.create(children.get(i));
                Node nodeList = new Node();
                processNode(child, scene, nodeList, materials);
                s.attachChild(nodeList);
            }
        }

    }

    private static void processNodeMeshes(AIScene scene, AINode node, Node n, List<Material> materials) {
        IntBuffer meshIndices = node.mMeshes();
        for (int i = 0; i < meshIndices.capacity(); i++) {
            processMesh(scene, meshIndices.get(i), n, materials);
        }
    }

    private static List<AssimpMaterial> processMaterials(AIScene scene) {
        PointerBuffer pMaterials = scene.mMaterials();
        int numMaterials = scene.mNumMaterials();
        ArrayList<AssimpMaterial> materials = new ArrayList<>(numMaterials);
        AIColor4D colour = AIColor4D.create();
        AIString string = AIString.create();
        float[] floats = new float[4];
        int[] ints = new int[4];
        int[] arraysLimits = new int[]{4};

        for (int i = 0; i < numMaterials; i++) {
            AIMaterial material = AIMaterial.create(pMaterials.get(i));
            AssimpMaterial m = new AssimpMaterial();
            materials.add(m);
//            int numProperties = material.mNumProperties();
//            PointerBuffer pMaterialProperties = material.mProperties();
//            for (int j = 0; j < numProperties; j++) {
//                AIMaterialProperty materialProperty = AIMaterialProperty.create(pMaterialProperties.get(j));
//                String key = materialProperty.mKey().dataString();
//                Object value = null;
//
//                System.out.println(key + ": " + materialProperty.mType() + "    " + value);
//            }
//            System.out.println();


            for (int type = 1; type <= 21; type++) {
                int count = aiGetMaterialTextureCount(material, type);
                if (count != 0) {
                    if (m.textures.isEmpty())
                        m.textures = new ArrayList<>(count);
                    System.out.println("material has a texture of type: " + TextureType.valueOf(type) + "; count: " + count);
                    for (int j = 0; j < count; j++) {
                        aiGetMaterialTexture(material, type, j, string, (IntBuffer) null, null, null, null, null, null);
                        System.out.println("\t" + string.dataString());
                        m.textures.add(new TextureInfo(string.dataString(), TextureType.valueOf(type)));
                    }
                }
            }


            int result = aiGetMaterialColor(material, AI_MATKEY_COLOR_AMBIENT, aiTextureType_NONE, 0, colour);
            if (result == 0) {
                m.ambientColor = new Vector4f(colour.r(), colour.g(), colour.b(), colour.a());
            }
            result = aiGetMaterialColor(material, AI_MATKEY_COLOR_DIFFUSE, aiTextureType_NONE, 0, colour);
            if (result == 0) {
                m.diffuseColor = new Vector4f(colour.r(), colour.g(), colour.b(), colour.a());
            }
            result = aiGetMaterialColor(material, AI_MATKEY_COLOR_EMISSIVE, aiTextureType_NONE, 0, colour);
            if (result == 0) {
                m.emissiveColor = new Vector4f(colour.r(), colour.g(), colour.b(), colour.a());
            }
            result = aiGetMaterialColor(material, AI_MATKEY_COLOR_REFLECTIVE, aiTextureType_NONE, 0, colour);
            if (result == 0) {
                m.reflectiveColor = new Vector4f(colour.r(), colour.g(), colour.b(), colour.a());
            }
            result = aiGetMaterialColor(material, AI_MATKEY_COLOR_SPECULAR, aiTextureType_NONE, 0, colour);
            if (result == 0) {
                m.specularColor = new Vector4f(colour.r(), colour.g(), colour.b(), colour.a());
            }
            result = aiGetMaterialColor(material, AI_MATKEY_COLOR_TRANSPARENT, aiTextureType_NONE, 0, colour);
            if (result == 0) {
                m.transparentColor = new Vector4f(colour.r(), colour.g(), colour.b(), colour.a());
            }
            result = aiGetMaterialString(material, AI_MATKEY_NAME, aiTextureType_NONE, 0, string);
            if (result == 0) {
                m.name = string.dataString();
            }
            result = aiGetMaterialString(material, AI_MATKEY_GLTF_ALPHAMODE, aiTextureType_NONE, 0, string);
            if (result == 0) {
                m.alphamode = string.dataString();
            }

            arraysLimits[0] = floats.length;
            result = aiGetMaterialFloatArray(material, AI_MATKEY_SHININESS, aiTextureType_NONE, 0, floats, arraysLimits);
            if (result == 0) {
                m.shininess = floats[0];
            }
            arraysLimits[0] = floats.length;
            result = aiGetMaterialFloatArray(material, AI_MATKEY_METALLIC_FACTOR, aiTextureType_NONE, 0, floats, arraysLimits);
            if (result == 0) {
                m.metallicFactor = floats[0];
            }
            arraysLimits[0] = floats.length;
            result = aiGetMaterialFloatArray(material, AI_MATKEY_ROUGHNESS_FACTOR, aiTextureType_NONE, 0, floats, arraysLimits);
            if (result == 0) {
                m.roughnessFactor = floats[0];
            }
            arraysLimits[0] = floats.length;
            result = aiGetMaterialFloatArray(material, AI_MATKEY_OPACITY, aiTextureType_NONE, 0, floats, arraysLimits);
            if (result == 0) {
                m.opacity = floats[0];
            }
            arraysLimits[0] = floats.length;
            result = aiGetMaterialFloatArray(material, AI_MATKEY_GLTF_ALPHACUTOFF, aiTextureType_NONE, 0, floats, arraysLimits);
            if (result == 0) {
                m.alphacutoff = floats[0];
            }
            arraysLimits[0] = floats.length;
            result = aiGetMaterialIntegerArray(material, AI_MATKEY_SHADING_MODEL, aiTextureType_NONE, 0, ints, arraysLimits);
            if (result == 0) {
                m.shadingModel = ints[0];
            }

        }
        return materials;
    }

    private static void processMesh(AIScene scene, int meshIndex, Node model, List<Material> materials) {

        PointerBuffer pMeshes = scene.mMeshes();
        AIMesh mesh = AIMesh.create(pMeshes.get(meshIndex));

        Material material = materials.get(mesh.mMaterialIndex());

        Geometry geometry = new Geometry(
                mesh.mName().dataString(),
                readMesh(mesh),
                material
        );
        model.attachChild(geometry);
    }

    private static Mesh readMesh(AIMesh mesh) {
        AIVector3D.Buffer positions = requireNonNull(mesh.mVertices());
        AIVector3D.Buffer normals = mesh.mNormals();
        AIVector3D.Buffer tangents = mesh.mTangents();
        AIColor4D.Buffer colors = mesh.mColors(0);
        AIVector3D.Buffer textureCoords = mesh.mTextureCoords(0);

//        AIVector3D tempVector3D = AIVector3D.create();
//        AIColor4D tempColor = AIColor4D.create();

        Vertex[] vertices = new Vertex[positions.capacity()];
        for (int i = 0; i < vertices.length; i++) {
            AIVector3D position = positions.get(i);
            AIColor4D color = colors != null ? colors.get(i) : null;
            AIVector3D texCoords = textureCoords != null ? textureCoords.get(i) : null;
            AIVector3D normal = normals != null ? normals.get(i) : null;
            AIVector3D tangent = tangents != null ? tangents.get(i) : null;

            Vertex vertex = new Vertex(
                    new Vector3f(position.x(), position.y(), position.z()),
                    color != null ? new Vector4f(color.r(), color.g(), color.b(), color.a()) : null,
                    texCoords != null ? new Vector2f(texCoords.x(), texCoords.y()) : null,
                    normal != null ? new Vector3f(normal.x(), normal.y(), normal.z()) : null,
                    tangent != null ? new Vector3f(tangent.x(), tangent.y(), tangent.z()) : null
            );
            vertices[i] = vertex;
        }

        IntArrayBuilder indecies = new IntArrayBuilder((int) (vertices.length * 1.5));

        AIFace.Buffer aiFaces = mesh.mFaces();
        for (int i = 0; i < mesh.mNumFaces(); i++) {
            AIFace face = aiFaces.get(i);
            IntBuffer pIndices = face.mIndices();
            for (int j = 0; j < face.mNumIndices(); j++) {
                indecies.append(pIndices.get(j));
            }
        }

        Mesh m = new Mesh(vertices, indecies.toIntArray(true));

        AIAABB aabb = mesh.mAABB();
        if (aabb != null) {
            AIVector3D min = aabb.mMin();
            AIVector3D max = aabb.mMax();
            m.setBoundingBox(new Mesh.BoundingBox(
                    new Vector3f(min.x(), min.y(), min.z()),
                    new Vector3f(max.x(), max.y(), max.z())
            ));
        }

        return m;
    }


    public static class AssimpMaterial {
        public String name;
        public Vector4f ambientColor;
        public Vector4f diffuseColor;
        public Vector4f emissiveColor;
        public Vector4f reflectiveColor;
        public Vector4f specularColor;
        public Vector4f transparentColor;
        public String alphamode;
        public float shininess;
        public float metallicFactor;
        public float roughnessFactor;
        public float opacity;
        public float alphacutoff;
        public int shadingModel;
        public List<TextureInfo> textures = Collections.emptyList();
    }

    public static class TextureInfo {
        public final String fileName;
        public final TextureType type;

        public TextureInfo(String fileName, TextureType type) {
            this.fileName = fileName;
            this.type = type;
        }
    }

    public enum TextureType {
        NONE(0),
        DIFFUSE(1),
        SPECULAR(2),
        AMBIENT(3),
        EMISSIVE(4),
        HEIGHT(5),
        NORMALS(6),
        SHININESS(7),
        OPACITY(8),
        DISPLACEMENT(9),
        LIGHTMAP(10),
        REFLECTION(11),
        BASE_COLOR(12),
        NORMAL_CAMERA(13),
        EMISSION_COLOR(14),
        METALNESS(15),
        DIFFUSE_ROUGHNESS(16),
        AMBIENT_OCCLUSION(17),
        SHEEN(19),
        CLEARCOAT(20),
        TRANSMISSION(21),
        UNKNOWN(18);

        public final int assimpType;

        private static final TextureType[] byAssimpType = new TextureType[22];

        TextureType(int assimpType) {
            this.assimpType = assimpType;
        }

        static {
            for (TextureType type : values()) {
                byAssimpType[type.assimpType] = type;
            }
        }

        public static TextureType valueOf(int assimpType) {
            return byAssimpType[assimpType];
        }

        public TextureImage.Type toTextureImageType() {
            return TextureImage.Type.valueOf(name());
        }
    }

}
