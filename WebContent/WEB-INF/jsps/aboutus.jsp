<%@ page language="java" import="java.util.*" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jstl/core_rt" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="content-type" content="text/html; charset=UTF-8" />
 <!-- 手机适配开始  -->
<meta name="applicable-device" content="mobile" />
<meta name="viewport"
	content="initial-scale=1, width=device-width, maximum-scale=1, user-scalable=no" />
<meta name="apple-mobile-web-app-capable" content="yes" />
<meta name="apple-touch-fullscreen" content="yes" />
<meta name="apple-mobile-web-app-status-bar-style" content="black" />
<link rel="shortcut icon" type="image/x-icon" href="../staticfile/images/haire.png"/>
<meta name="format-detection" content="telephone=no" />
<meta name="format-detection" content="address=no" />
<!-- 手机适配结束  -->
 <title>美美发圈</title>
<style>
/* GENERAL DEFINITIONS */
* html body { behavior: url("/htc/csshover.htc"); } /* FOR IE6 */
* +html body { behavior: url("/htc/csshover.htc"); } /* FOR IE7 */
body { padding: 0; margin: 0; font: 12px "ËÎÌå", Arial; line-height: 22px; color: #666; background-color: #FFF; }
form { margin: 0; padding: 0; }
input,select,textarea { font-size: 12px; background-color: #FFF; color: #666; }
p { margin: 0 0 15px; }
img { border: 0; }
a { color: #666; text-decoration: none; }
a:hover { color: #666; text-decoration: underline; }
dl, dt, dd, ul { margin: 0; padding: 0; }
ul li { list-style: none; }
em, i { font-style: normal; }

#link2m i { display: none; margin-left: 3px; color: #CCC; font-style: normal; }
#link2m img { display: none; width: 140px; }
#link2m:hover { position: absolute; z-index: 800; margin-left: -6px; padding: 5px; width: 140px; height: auto; background-color: #FFF; border: 1px solid #DCDCDC; border-top: none; text-decoration: none; }
#link2m:hover i, #link2m:hover img { display: inline-block; }


#header { width: 100%; background-color: #FFF; color: #B3B3B3; overflow: hidden; }
#header p { display: block; margin: 5px auto; width: 1000px; height: 65px; overflow: hidden; }
#header a.logo { float: left; display: inline; margin: 10px 10px 0 0; width: 168px; height: 46px; background: url(../images/logo.jpg) no-repeat; border: none; text-indent: -2000px; }
#header a { float: right; margin: 0 0 0 3px; border: 1px solid #EEE; }
#header a img { height: 63px; }

/* NAVIGATION BAR */
#nav { margin: 0 auto; padding: 8px; width: 984px; line-height: 20px; background-color: #3D3D3D; overflow: hidden; }
#nav a { text-decoration: none; }
#nav a:hover { text-decoration: underline; }
#nav dl { float: left; margin: 0; padding: 0; height: 40px; overflow: hidden; }
#nav dt { float: left; margin: 0; padding: 9px; height: 23px; background: url(../images/nav.gif) right -128px no-repeat; color: #957E5E; font-size: 16px; font-weight: bold; }
#nav dt.no_child { background: none; }
#nav dt.par2 { background: url(../images/nav.gif) right -30px no-repeat; }
#nav dt a { float: left; color: #957E5E; font-size: 16px; font-weight: bold; }
#nav dt a:hover { color: #957E5E; }
#nav dd { float: left; margin: 0; padding: 0 0 0 5px; }
#nav ul { margin: 0; padding: 0; }
#nav li { float: left; display: block; padding: 0 8px; }
#nav li.par { background: url(../images/nav.gif) right -38px no-repeat; }
#nav li a { color: #E0DFE0; font-size: 12px; font-weight: bold; }
#nav li a:hover { color: #E0DFE0; }
#nav li.br { clear: both; float: none; padding: 0; border-bottom: none; }
* html #nav li { display: inline; } /* FOR IE6 */
*+html #nav li { display: inline; } /* FOR IE7 */

#nav li span.more { color: #E0DFE0; font-size: 12px; font-weight: bold; cursor: pointer; }
#nav li p.more { display: none; position: absolute; z-index: 500; margin: 0; padding: 10px 5px; line-height: 25px; background-color: #FFF; border: 1px solid #CECECE; }
#nav li p.more a { display: inline-block; margin: 0 10px; color: #3D3D3D; font-weight: normal; }
#nav li p.more a:hover { color: #3D3D3D; }
#nav li p.more span { display: block; position: absolute; top: -14px; padding-top: 5px; width: 100%; }
#nav li p.more i { display: block; margin: 0 auto; width: 15px; height: 9px; background: url(../images/nav.gif) left top no-repeat; }
* html #nav li p.more { width: 250px; } /* FOR IE6 */
*+html #nav li p.more { width: 250px; } /* FOR IE7 */

/* CONTAINER */
#container { margin: 5px auto; padding: 0; width: 400px; overflow: hidden; }

.menu { float: left; width: 70px; }
.menu  li { float: left; display: inline; margin-bottom: 10px; width: 70px; }
.menu  li a, .menu  li b { display: block; padding: 7px; width: 56px; height: 66px; line-height: 25px; font-size: 24px; font-family: "Î¢ÈíÑÅºÚ", "ËÎÌå"; font-weight: normal; text-align: center; overflow: hidden; }
.menu  li a { background: url(../images/icon.gif) left top no-repeat; color: #AFAFAF; }
.menu  li a:hover, .menu  li b { background: url(../images/icon.gif) right top no-repeat; color: #FFF; text-decoration: none; }

.slogan { float: left; position: relative; top: -2px; margin: 0 40px 40px; padding-right: 110px; height: 47px; line-height: 47px; background: url(../images/bg.gif) right bottom no-repeat; color: #AFAFAF; font-size: 14px; font-family: "Î¢ÈíÑÅºÚ", "ËÎÌå"; text-align: right; }
.slogan i { margin: 0 3px; color: #E60044; font-size: 20px; }

.right { float: left; padding: 30px 60px; width: 703px; background-color: #EDECEB; border-bottom: 2px solid #C5C5C5; border-right: 2px solid #C5C5C5; border-radius: 15px; -moz-border-radius: 15px; overflow: hidden; }
.right h1 { float: left; margin: 0 0 20px; padding: 10px 0; width: 100%; color: #000; border-bottom: 1px solid #E4E4E4; font-size: 28px; font-family: "Î¢ÈíÑÅºÚ", "ËÎÌå"; font-weight: normal; }
.right h1 span { margin: 0 3px; color: #C5C5C5; font-size: 18px; font-family: Arial; }
.right .s1 { float: left; width: 100%; }
.right .s1 dt { margin-bottom: 10px; color: #957E5E; font-size: 16px; }
.right .s1 dd { margin-bottom: 30px; }

.right .s2 { float: left; display: inline-block; width: 718px; }
.right .s2 dl { float: left; display: inline-block; margin-bottom: 30px; margin-right: 15px; width: 344px; line-height: 30px; }
.right .s2 dl.br { clear: both; }
.right .s2 dt { margin-bottom: 10px; border-bottom: 1px dashed #D5D5D5; color: #957E5E; font-size: 18px; }
.right .s2 dd { display: inline-block; width: 100%; }
.right .s2 dd img { float: left; margin-right: 10px; }
.right .s2 dd .wx { display: inline-block; padding-top: 20px; line-height: 25px; font-size: 14px; }
.right .s2 dd .place, .right .s2 dd .tel, .right .s2 dd .fax, .right .s2 dd .site, .right .s2 dd .email, .right .s2 dd .mb, .right .s2 dd .man { float: left; display: block; margin-right: 5px; width: 22px; height: 22px; background-image: url(../images/icon.gif); background-repeat: no-repeat; text-indent: 100px; overflow: hidden; }
.right .s2 dd .place { background-position: 0 -163px; }
.right .s2 dd .tel { background-position: 0 -185px; }
.right .s2 dd .fax { background-position: 0 -207px; }
.right .s2 dd .site { background-position: 0 -229px; }
.right .s2 dd .email { background-position: 0 -250px; }
.right .s2 dd .mb { background-position: 0 -271px; }
.right .s2 dd .man { background-position: 0 -296px; }
.right h2 { float: left; margin: 0; padding: 10px 0; width: 100%; background-image: url(../images/icon.gif); background-repeat: no-repeat; text-indent: 32px; }
.right h2.brand { background-position: -175px -80px; }
.right h2.trade { background-position: -175px -116px; }
.right h2.news { background-position: -175px -152px; }
.right h2.shop { background-position: -175px -188px; }
.right h2.area { background-position: -175px -296px; }
.right h2.q { background-position: -175px -224px; }
.right h2.user { background-position: -175px -260px; }
.right h2 a { color: #957E5E; font-size: 18px; font-weight: normal; }
.right .s3 { float: left; margin-bottom: 20px; padding: 20px 0 0; width: 701px; background-color: #FBFBFB; border: 1px solid #F1F1F1; }
.right .s3 a { display: inline-block; margin-right: 5px; color: #999; }
.right .s3 dl { float: left; padding: 0 20px 20px; width: 310px; }
.right .s3 dt a { color: #000; font-size: 14px; }
.right .s3 dd.mb20px { margin-bottom: 20px; }
.right .s3 dd a.b { color: #000; }

.right .options { float: left; width: 100%; }
.right .options dt { display: inline-block; margin-bottom: 15px; }
.right .options dt span { display: inline-block; margin-right: 10px; width: 131px; height: 31px; line-height: 31px; background: url(../images/icon.gif) left -88px no-repeat; cursor: pointer; }
.right .options dt span a { display: inline-block; width: 100%; height: 100%; }
.right .options dt span a:hover { text-decoration: none; }
.right .options dt span i { float: left; display: inline-block; padding: 0 15px; font-size: 18px; font-family: Arial; color: #FFF; }
.right .options dt span.current { background: url(../images/icon.gif) left -125px no-repeat; color: #FFF; }
.right .options dt span.current i { color: #000; }
.right .options dd { display: none; position: relative; padding: 10px; width: 683px; background-color: #FFF; color: #957E5E; }
.right .options dd.current { display: block; }

.list dl { float: left; width: 100%; }
.list dl.current dd { display: block; }
.list dt { display: inline; line-height: 35px; color: #957E5E; font-size: 16px; cursor: pointer; }
.list dt:hover { text-decoration: underline; }
.list dd { display: none; margin-bottom: 30px; padding-left: 17px; }
.list ol { margin: 0 0 20px 0; padding: 0 0 0 30px; }

.wx .p1 { color: #000; font-size: 22px; font-family: "Î¢ÈíÑÅºÚ", "ËÎÌå"; font-weight: normal; }
.wx .p2 { margin-bottom: 30px; padding: 10px; background-color: #FFF; }
.wx .p3 { color: #000; }
.wx .p4 span { float: left; margin-right: 10px; margin-bottom: 10px; padding: 0 7px; background-color: #E60044; color: #FFF; border-radius: 3px; -moz-border-radius: 3px; }
.wx .p5 { display: inline-block; width: 703px; background: url(../images/to.gif) 350px 120px no-repeat; }
.wx .p5 span { float: left; width: 50%; text-align: center; }
.wx .p5 span img { margin-bottom: 10px; }

</style>

</head>
<body>
<div id="container">
	<div class="right">
		<h1>美美发圈<span>ABOUT MMFQ</span></h1>
		<dl class="s1">
			<dt><b>关于我们</b></dt>
			<dd>&nbsp;&nbsp;&nbsp;&nbsp;致力于让美发生活更便利，更美好，更贴心！<br>
			    &nbsp;&nbsp;&nbsp;&nbsp;让大大的城市里面装着小小的温暖。<br>
			    &nbsp;&nbsp;&nbsp;&nbsp;聚焦于美发排队等候的困扰，打造美发的贴心管家。
			</dd><dt><b>我们的宗旨</b></dt>
			<dd>
			&nbsp;&nbsp;&nbsp;&nbsp;<strong>在家</strong>中就可以知晓理发店的排队情况。<br>
			&nbsp;&nbsp;&nbsp;&nbsp;<strong>微信上</strong>手指点一点就可以预约排号。<br>
			&nbsp;&nbsp;&nbsp;&nbsp;等候的同时做你喜欢的事情，管家温馨提醒你入店。<br>
		    <dt><b>团队组成</b></dt>
			<dd>&nbsp;&nbsp;&nbsp;&nbsp;美美发圈平台由<strong>成都冬橙科技有限公司</strong>倾力打造。<br>
			&nbsp;&nbsp;&nbsp;&nbsp;由一群热爱生活，享受生活，追求生活的年轻人组成。<br>
			&nbsp;&nbsp;&nbsp;&nbsp;我们相信享受生活的人才能引领生活！</dd>
			<dt><b>价值观</b></dt>
			<dd>&nbsp;&nbsp;&nbsp;&nbsp;正如这颗“❤”是我们的LOGO<br>
			&nbsp;&nbsp;&nbsp;&nbsp;正如我们宗旨“我们一直在认真精致地生活着”</dd>
		</dl>
	</div>
	<div class="slogan">享受<i>生活</i>每一天，我们一直在认真精致地生活着！</div>
</div>
</body>

</html>