export default ({icon}) => {
    return <i className="material-icons" dangerouslySetInnerHTML={{__html: `&#x${icon};`}}/>
}