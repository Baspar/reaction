(ns reaction.style)

(def style
  "
.pit-plugin--actions-card--title{
    text-align: center;
    color: #4a4a4a;
    margin-top: 1em;
    font-size: 20px;
}
.pit-plugin--actions-card--sub-title{
    text-align: center;
    color: #4a4a4a;
    font-weight: thin;
    font-size: 12px;
}

.pit-plugin--namespace{
    border: 1px solid rgb(231,234,242);
    border-radius: 3px;
    overflow: hidden;
    margin-top: 1em;
    margin-bottom: 1em;
}
.pit-plugin--namespace-name{
    text-align: center;
    color: #666;
    background-color: rgb(239, 237, 237);
    padding: 5px 0;
    font-size: 25px;
    line-height: normal;
}
.pit-plugin--actions{
    padding: 5px 10px;
}
.pit-plugin--action-name{
    font-size: 15px;
    transition: background-color .3s;
}
.pit-plugin--action-name b{
    color: #1265a4;
}
.pit-plugin--action-name--container{
    transition: background-color .3s;
}
.pit-plugin--action-name--container.hidden{
    background-color: transparent;
}
.pit-plugin--action-name--container.visible{
    background-color: rgb(239, 237, 237);
}

.pit-plugin--line-number{
    margin-left: 10px;
    font-size: 10px;
    color: lightgrey;
}

.pit-plugin--params{
    font-size: 12px;
    font-family: SCSansBold;
}
.pit-plugin--documentation{
    white-space:pre-wrap;
}
.pit-plugin--drawer{
    max-height: 0px;
    overflow: hidden;

    background-color: rgb(239, 237, 237);
    padding: 0px 10px;
    font-size: 12px;
    font-family: SCSansThin;
}

.pit-plugin--action{
    transition: background-color .3s;
    cursor: pointer;
}
.pit-plugin--action:hover{
    background-color: #fafafa;
}
.pit-plugin--action-name--container{
    display: flex;
}

.pit-plugin--action-name--container.visible ~ .pit-plugin--drawer {
    max-height: 500px;
    padding: 5px 10px;
}

.pit-plugin--action-bang{
    display: flex;
    width: 1.5em;
    height: 1.5em;
    justify-content: center;
    align-items: center;
    font-family: SCSansBold;
    color: white;
    border-radius: 50%;
    transform: scale(0.6);
}
.pit-plugin--action-bang.enabled{
    background-color: grey;
}

#pit-plugin--actions-container{
    max-height: 100%;
    overflow: auto;
}
.pit-plugin--actions-card{
    display: flex;
    justify-content: space-between;
    font-family: SCSansRegular;
    position: fixed;
    top: 0;
    bottom: 0;
    left: 0;
    right: 0;
    pointer-events: none;
}
.pit-plugin--actions-card--right{
    display: flex;
    flex-direction: column;
    transform: translateX(510px);
    z-index: 424242;
    background-color: white;
    height: 100vh;
    width: 500px;
    padding: 0px 10px;
    box-sizing: border-box;
    transition: transform .3s ease-in-out;
    box-shadow: 0 1px 9px 0 rgba(0,0,0,0.5);
}
.pit-plugin--actions-card--right.visible{
    transform: translateX(0px);
    pointer-events: all;
}
.pit-plugin--actions-card--left{
    display: flex;
    flex-direction: column;
    transform: translateX(-510px);
    z-index: 424242;
    background-color: white;
    height: 100vh;
    width: 500px;
    padding: 0px 10px;
    box-sizing: border-box;
    transition: transform .3s ease-in-out;
    box-shadow: 0 1px 9px 0 rgba(0,0,0,0.5);
}
.pit-plugin--actions-card--left.visible{
    transform: translateX(0px);
    pointer-events: all;
}

.pit-plugin--no-result--placeholder{
    font-family: SCSansThin;
    font-size: 30px;
    line-height: normal;
    color: #4A4A4A;
    text-align: center;
    margin: 2em;
}

.pit-plugin--no-documentation{
    margin-left: 2em;
    font-family: SCSansThin;
    color: orange;
}
.pit-plugin--input--container{
    border-bottom: solid 1px lightgrey;
    width: 80%;
    margin: 1em 10%;
    display: flex;
}
#pit-plugin--actions-card--input{
    flex: 1;
    background-color: transparent;
    outline: none;
    border: none;
    padding: 5px 10px;
    font-family: SCSansThin;
}
.pit-plugin--input--cross{
    height: 1.5em;
    width: 1.5em;
    border-radius: 50%;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: transparent;
    transition: background-color .3s;
}
.pit-plugin--input--cross:hover{
    background-color: lightgrey;
}
.pit-plugin--input--cross:active{
    background-color: grey;
}
.pit-plugin--actions-list{
    flex: 1;
    overflow: auto;
}
.pit-plugin--actions-card--right .pit-plugin--close-button{
    cursor: pointer;
    align-self: flex-start;
    margin-top: 10px;
}
.pit-plugin--actions-card--left .pit-plugin--close-button{
    cursor: pointer;
    align-self: flex-end;
    margin-top: 10px;
}
.pit-plugin--column{
    display: flex;
    flex-direction: column;
    width: 100%;
    max-height: 100%;
}
.pit-plugin--action-history--container{
    flex: 1;
    overflow: auto;
}
.pit-plugin--pointer{
    cursor: pointer;
}
.pit-plugin--action-history-element{
    display: flex;
    background-color: rgb(239, 237, 237);
    color: #666;
    border-radius: 5;
    margin: 5px 10px;
    padding: 5px 10px;
}
.pit-plugin--action-history-time{
    width: 8em;
}
.pit-plugin--action-history-params{
    margin-left: 10px;
}")
