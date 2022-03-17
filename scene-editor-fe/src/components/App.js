import React from 'react';
import './App.css'
import {classNames} from "react-ui-basics/Tools";
import Dialog from "./Dialog";
import Button from "react-ui-basics/Button";

const isJavaFX = () => navigator.userAgent.indexOf('JavaFX') !== -1;

export default () => {
    console.log('App.render()');
    return (
        <div className={classNames("App", isJavaFX() && '_transparent')}>
            {navigator.userAgent} <br/>
            {window.innerWidth}x{window.innerHeight} <br/>

            <Button onClick={e => {
                console.log("click");
                window.javaBridge?.onCommand('commandName', JSON.stringify({data: 'value'}))
            }}>click me</Button>
            <Dialog/>
        </div>
    );
}

