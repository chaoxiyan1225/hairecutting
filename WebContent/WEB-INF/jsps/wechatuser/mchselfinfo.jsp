<%@ page language="java" import="java.util.*" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jstl/core_rt" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="content-type" content="text/html; charset=UTF-8" />
<meta charset="utf-8" />
<title>美美发圈</title>
<!-- 手机适配开始  -->
<meta name="applicable-device" content="mobile" />
<meta name="viewport"
	content="initial-scale=1, width=device-width, maximum-scale=1, user-scalable=no" />
<meta name="apple-mobile-web-app-capable" content="yes" />
<meta name="apple-touch-fullscreen" content="yes" />
<meta name="apple-mobile-web-app-status-bar-style" content="black" />
<link rel="shortcut icon" type="image/x-icon" href="../staticfile/images/xianhua.jpg"/>
<meta name="format-detection" content="telephone=no" />
<meta name="format-detection" content="address=no" />
<!-- 手机适配结束  -->
<link href="../staticfile/styles/food_front.css"
	rel="stylesheet" onload="MT.pageData.eveTime=Date.now()" />
<link href="../staticfile/styles/singlefood.css"
	rel="stylesheet" onload="MT.pageData.eveTime=Date.now()" />
<link rel="stylesheet" type="text/css" href="../staticfile/styles/index_new.css">
<link rel="stylesheet" type="text/css"
	href="../staticfile/styles/screen.css">
<script src="../staticfile/commonjs/jquery-1.9.1.min.js"></script>
</head>
<body class="automove" id="deal-detail" data-com="pagecommon"
	data-page-id="100009" data-iswebview="false">
	<header class="navbar">
		<span class="nav-header h1"
			style="background-color:red; font-size: 0.5rem;">尊敬的商家${mchUser.mchName}</span>
	</header>
	<div id="deal" class="deal">
		<div class="list">
			<dl class="list list-in">
				<dd class="dd-padding buy-price" data-com="sticky"
					data-distance="1.01">
				</dd>
			</dl>
		</div>
		<div id="feedback_async" style="margin-top: .2rem">
			<dl style="opacity: 1;" id="deal-terms" class="list">
				<dt gaevent="imt/deal/terms">
					<b>商家档案</b>
				</dt>
				<dd class="dd-padding">
					<ul>
						<li>
							<div class="tip-title">
								<span style="color: red">商户ID：<font color="blank">${mchUser.mchId}</font></span>
							</div>
						</li>
						<li>
							<div class="tip-title">
								<span style="color: red">级别  : <font color="blank">${mchUser.mchLelvel}</font></span>
							</div>
						</li>
						<li>
							<div class="tip-title">
								<span style="color: red">微信ID    : <font color="blank">${mchUser.fkOpenId}</font></span>
							</div>
						</li>
						<li>
							<div class="tip-title">
								<span style="color: red">商家积分    : <font color="blank">${mchUser.points}</font></span>
							</div>
						</li>
						<li>
							<div class="tip-title">
								<span style="color: red">商家积服务状态   : <font color="blank">${mchUser.serviceStatus}</font></span>
							</div>
						</li>
						<li>
							<div class="tip-title">
								<span style="color: red">注册时间  : <font color="blank">${mchUser.registerTime}</font></span>
							</div>
						</li>
						
			            <li>
							<div class="tip-title">
								<span style="color: red">订单备注 : </span>
							</div>
							<div class="tip-des">${cacheUser.mchUser.shopDetail}</div>
						</li>
					</ul>
				</dd>
			</dl>
		</div>
	</div>
</body>

 <script type="text/javascript">
    
	function showMessage(message,type,time) {
	    let str = '';
	    switch (type) {
	        case 'success':
	            str = '<div class="success-message" style="width: 300px;height: 40px;text-align: center;background-color:#daf5eb;;color: rgba(59,128,58,0.7);position: fixed;left: 43%;top: 10%;line-height: 40px;border-radius: 5px;z-index: 9999">\n' +
	                '    <span class="mes-text">'+message+'</span></div>';
	            break;
	        case 'error':
	            str = '<div class="error-message" style="width: 300px;height: 40px;text-align: center;background-color: #f5f0e5;color: rgba(238,99,99,0.8);position: fixed;left: 43%;top: 10%;line-height: 40px;border-radius: 5px;;z-index: 9999">\n' +
	                '    <span class="mes-text">'+message+'</span></div>';
	    }
	    $('body').append(str);
	    setTimeout(function () {
	        $('.'+type+'-message').remove()
	    },time);
	};
   
    </script>

<script type="text/javascript"
	src="../staticfile/commonjs/jquery.lazyload.min.js"></script>
<script type="text/javascript"
	src="../staticfile/commonjs/shared.jquery.js"></script>
</html>