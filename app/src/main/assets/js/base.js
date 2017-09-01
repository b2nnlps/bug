/*
 API公共库
 基础库，会带登录
 */

function getQueryString(name) { //获取get传过来的用户信息
    var reg = new RegExp("(^|&)" + name + "=([^&]*)(&|$)", "i");
    var r = window.location.search.substr(1).match(reg);
    if (r != null) return decodeURI(r[2]);
    return "";
} 