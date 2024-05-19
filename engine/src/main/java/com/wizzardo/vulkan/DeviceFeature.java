package com.wizzardo.vulkan;

import org.lwjgl.vulkan.VkPhysicalDeviceFeatures;

public enum DeviceFeature {

     ROBUST_BUFFER_ACCESS,
     FULL_DRAW_INDEX_UINT32,
     IMAGE_CUBE_ARRAY,
     INDEPENDENT_BLEND,
     GEOMETRY_SHADER,
     TESSELLATION_SHADER,
     SAMPLE_RATE_SHADING,
     DUAL_SRC_BLEND,
     LOGIC_OP,
     MULTI_DRAW_INDIRECT,
     DRAW_INDIRECT_FIRST_INSTANCE,
     DEPTH_CLAMP,
     DEPTH_BIAS_CLAMP,
     FILL_MODE_NON_SOLID,
     DEPTH_BOUNDS,
     WIDE_LINES,
     LARGE_POINTS,
     ALPHA_TO_ONE,
     MULTI_VIEWPORT,
     SAMPLER_ANISOTROPY,
     TEXTURE_COMPRESSION_ETC2,
     TEXTURE_COMPRESSION_ASTC_LDR,
     TEXTURE_COMPRESSION_BC,
     OCCLUSION_QUERY_PRECISE,
     PIPELINE_STATISTICS_QUERY,
     VERTEX_PIPELINE_STORES_AND_ATOMICS,
     FRAGMENT_STORES_AND_ATOMICS,
     SHADER_TESSELLATION_AND_GEOMETRY_POINT_SIZE,
     SHADER_IMAGE_GATHER_EXTENDED,
     SHADER_STORAGE_IMAGE_EXTENDED_FORMATS,
     SHADER_STORAGE_IMAGE_MULTI_SAMPLE,
     SHADER_STORAGE_IMAGE_READ_WITHOUT_FORMAT,
     SHADER_STORAGE_IMAGE_WRITE_WITHOUT_FORMAT,
     SHADER_UNIFORM_BUFFER_ARRAY_DYNAMIC_INDEXING,
     SHADER_SAMPLED_IMAGE_ARRAY_DYNAMIC_INDEXING,
     SHADER_STORAGE_BUFFER_ARRAY_DYNAMIC_INDEXING,
     SHADER_STORAGE_IMAGE_ARRAY_DYNAMIC_INDEXING,
     SHADER_CLIP_DISTANCE,
     SHADER_CULL_DISTANCE,
     SHADER_FLOAT64,
     SHADER_INT64,
     SHADER_INT16,
     SHADER_RESOURCE_RESIDENCY,
     SHADER_RESOURCE_MIN_LOD,
     SPARSE_BINDING,
     SPARSE_RESIDENCY_BUFFER,
     SPARSE_RESIDENCY_IMAGE2D,
     SPARSE_RESIDENCY_IMAGE3D,
     SPARSE_RESIDENCY_2SAMPLES,
     SPARSE_RESIDENCY_4SAMPLES,
     SPARSE_RESIDENCY_8SAMPLES,
     SPARSE_RESIDENCY_16SAMPLES,
     SPARSE_RESIDENCY_ALIASED,
     VARIABLE_MULTI_SAMPLE_RATE,
     INHERITED_QUERIES;

     public void enable(VkPhysicalDeviceFeatures physicalDeviceFeatures) {
          switch (this) {
               case ROBUST_BUFFER_ACCESS:
                    physicalDeviceFeatures.robustBufferAccess(true);
                    break;
               case FULL_DRAW_INDEX_UINT32:
                    physicalDeviceFeatures.fullDrawIndexUint32(true);
                    break;
               case IMAGE_CUBE_ARRAY:
                    physicalDeviceFeatures.imageCubeArray(true);
                    break;
               case INDEPENDENT_BLEND:
                    physicalDeviceFeatures.independentBlend(true);
                    break;
               case GEOMETRY_SHADER:
                    physicalDeviceFeatures.geometryShader(true);
                    break;
               case TESSELLATION_SHADER:
                    physicalDeviceFeatures.tessellationShader(true);
                    break;
               case SAMPLE_RATE_SHADING:
                    physicalDeviceFeatures.sampleRateShading(true);
                    break;
               case DUAL_SRC_BLEND:
                    physicalDeviceFeatures.dualSrcBlend(true);
                    break;
               case LOGIC_OP:
                    physicalDeviceFeatures.logicOp(true);
                    break;
               case MULTI_DRAW_INDIRECT:
                    physicalDeviceFeatures.multiDrawIndirect(true);
                    break;
               case DRAW_INDIRECT_FIRST_INSTANCE:
                    physicalDeviceFeatures.drawIndirectFirstInstance(true);
                    break;
               case DEPTH_CLAMP:
                    physicalDeviceFeatures.depthClamp(true);
                    break;
               case DEPTH_BIAS_CLAMP:
                    physicalDeviceFeatures.depthBiasClamp(true);
                    break;
               case FILL_MODE_NON_SOLID:
                    physicalDeviceFeatures.fillModeNonSolid(true);
                    break;
               case DEPTH_BOUNDS:
                    physicalDeviceFeatures.depthBounds(true);
                    break;
               case WIDE_LINES:
                    physicalDeviceFeatures.wideLines(true);
                    break;
               case LARGE_POINTS:
                    physicalDeviceFeatures.largePoints(true);
                    break;
               case ALPHA_TO_ONE:
                    physicalDeviceFeatures.alphaToOne(true);
                    break;
               case MULTI_VIEWPORT:
                    physicalDeviceFeatures.multiViewport(true);
                    break;
               case SAMPLER_ANISOTROPY:
                    physicalDeviceFeatures.samplerAnisotropy(true);
                    break;
               case TEXTURE_COMPRESSION_ETC2:
                    physicalDeviceFeatures.textureCompressionETC2(true);
                    break;
               case TEXTURE_COMPRESSION_ASTC_LDR:
                    physicalDeviceFeatures.textureCompressionASTC_LDR(true);
                    break;
               case TEXTURE_COMPRESSION_BC:
                    physicalDeviceFeatures.textureCompressionBC(true);
                    break;
               case OCCLUSION_QUERY_PRECISE:
                    physicalDeviceFeatures.occlusionQueryPrecise(true);
                    break;
               case PIPELINE_STATISTICS_QUERY:
                    physicalDeviceFeatures.pipelineStatisticsQuery(true);
                    break;
               case VERTEX_PIPELINE_STORES_AND_ATOMICS:
                    physicalDeviceFeatures.vertexPipelineStoresAndAtomics(true);
                    break;
               case FRAGMENT_STORES_AND_ATOMICS:
                    physicalDeviceFeatures.fragmentStoresAndAtomics(true);
                    break;
               case SHADER_TESSELLATION_AND_GEOMETRY_POINT_SIZE:
                    physicalDeviceFeatures.shaderTessellationAndGeometryPointSize(true);
                    break;
               case SHADER_IMAGE_GATHER_EXTENDED:
                    physicalDeviceFeatures.shaderImageGatherExtended(true);
                    break;
               case SHADER_STORAGE_IMAGE_EXTENDED_FORMATS:
                    physicalDeviceFeatures.shaderStorageImageExtendedFormats(true);
                    break;
               case SHADER_STORAGE_IMAGE_MULTI_SAMPLE:
                    physicalDeviceFeatures.shaderStorageImageMultisample(true);
                    break;
               case SHADER_STORAGE_IMAGE_READ_WITHOUT_FORMAT:
                    physicalDeviceFeatures.shaderStorageImageReadWithoutFormat(true);
                    break;
               case SHADER_STORAGE_IMAGE_WRITE_WITHOUT_FORMAT:
                    physicalDeviceFeatures.shaderStorageImageWriteWithoutFormat(true);
                    break;
               case SHADER_UNIFORM_BUFFER_ARRAY_DYNAMIC_INDEXING:
                    physicalDeviceFeatures.shaderUniformBufferArrayDynamicIndexing(true);
                    break;
               case SHADER_SAMPLED_IMAGE_ARRAY_DYNAMIC_INDEXING:
                    physicalDeviceFeatures.shaderSampledImageArrayDynamicIndexing(true);
                    break;
               case SHADER_STORAGE_BUFFER_ARRAY_DYNAMIC_INDEXING:
                    physicalDeviceFeatures.shaderStorageBufferArrayDynamicIndexing(true);
                    break;
               case SHADER_STORAGE_IMAGE_ARRAY_DYNAMIC_INDEXING:
                    physicalDeviceFeatures.shaderStorageImageArrayDynamicIndexing(true);
                    break;
               case SHADER_CLIP_DISTANCE:
                    physicalDeviceFeatures.shaderClipDistance(true);
                    break;
               case SHADER_CULL_DISTANCE:
                    physicalDeviceFeatures.shaderCullDistance(true);
                    break;
               case SHADER_FLOAT64:
                    physicalDeviceFeatures.shaderFloat64(true);
                    break;
               case SHADER_INT64:
                    physicalDeviceFeatures.shaderInt64(true);
                    break;
               case SHADER_INT16:
                    physicalDeviceFeatures.shaderInt16(true);
                    break;
               case SHADER_RESOURCE_RESIDENCY:
                    physicalDeviceFeatures.shaderResourceResidency(true);
                    break;
               case SHADER_RESOURCE_MIN_LOD:
                    physicalDeviceFeatures.shaderResourceMinLod(true);
                    break;
               case SPARSE_BINDING:
                    physicalDeviceFeatures.sparseBinding(true);
                    break;
               case SPARSE_RESIDENCY_BUFFER:
                    physicalDeviceFeatures.sparseResidencyBuffer(true);
                    break;
               case SPARSE_RESIDENCY_IMAGE2D:
                    physicalDeviceFeatures.sparseResidencyImage2D(true);
                    break;
               case SPARSE_RESIDENCY_IMAGE3D:
                    physicalDeviceFeatures.sparseResidencyImage3D(true);
                    break;
               case SPARSE_RESIDENCY_2SAMPLES:
                    physicalDeviceFeatures.sparseResidency2Samples(true);
                    break;
               case SPARSE_RESIDENCY_4SAMPLES:
                    physicalDeviceFeatures.sparseResidency4Samples(true);
                    break;
               case SPARSE_RESIDENCY_8SAMPLES:
                    physicalDeviceFeatures.sparseResidency8Samples(true);
                    break;
               case SPARSE_RESIDENCY_16SAMPLES:
                    physicalDeviceFeatures.sparseResidency16Samples(true);
                    break;
               case SPARSE_RESIDENCY_ALIASED:
                    physicalDeviceFeatures.sparseResidencyAliased(true);
                    break;
               case VARIABLE_MULTI_SAMPLE_RATE:
                    physicalDeviceFeatures.variableMultisampleRate(true);
                    break;
               case INHERITED_QUERIES:
                    physicalDeviceFeatures.inheritedQueries(true);
                    break;
               default:
                    throw new IllegalStateException("Unexpected value: " + this);
          }

     }
}
