<%@ page language="java" import="java.util.*" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jstl/core_rt" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
<head>
<title>美美发圈</title>
<!-- 手机适配开始  -->
<meta name="applicable-device" content="mobile" />
<meta name="viewport"
	content="initial-scale=1, width=device-width, maximum-scale=1, user-scalable=no" />
<link rel="shortcut icon" type="image/x-icon" href="../staticfile/images/haire.png"/>
<meta name="apple-mobile-web-app-capable" content="yes" />
<meta name="apple-touch-fullscreen" content="yes" />
<meta name="apple-mobile-web-app-status-bar-style" content="black" />
<meta name="format-detection" content="telephone=no" />
<meta name="format-detection" content="address=no" />
<!-- 手机适配结束  -->
<link href="../staticfile/styles/food_front.css"
	rel="stylesheet" type="text/css" />
<link href="http://cdn.bootcss.com/font-awesome/4.7.0/css/font-awesome.min.css" rel="stylesheet"/>
<link rel="stylesheet" href="style.css"/>
<script type="text/javascript"
	src="../staticfile/commonjs/jquery-1.9.1.min.js"></script>
	<style type="text/css">
body {
	margin: 0;
	padding: 0;
	font-size: 14px;
	font-family: "microsoft yahei", 'Arial', 'Verdana', 'Helvetica',
		sans-serif;
}

form {
	position: relative;
	width: 300px;
	margin: 0 auto;
}

input,button {
	border: none;
	outline: none;
}

input {
	width: 90%;
	height: 30px;
	padding-left: 13px;
}

button {
	height: 42px;
	width: 42px;
	cursor: pointer;
	position: absolute;
}

.bar {
	position: fixed;
	top: 0;
	left: 0;
	right: 0; /* 决定了搜索框置顶 */
	height: 44px;
	padding: 0 10px;
	background-color: #fff;
	opacity: 0.8; /* 搜索框半透明效果 */
	z-index: 10;
}

.bar form {
	display: block;
	padding: 0;
	margin: 0;
}

.bar7 form {
	height: 42px;
}

.bar7 input {
	width: 250px;
	border-radius: 42px;
	border: 2px solid #324B4E;
	background: #F9F0DA;
	transition: .3s linear;
	float: right;
}

.bar7 input:focus {
	width: 280px;
}

.bar7 button {
	background: none;
	top: -2px;
	right: 0;
}

.bar7 button:before {
	content: "\f002";
	font-family: FontAwesome;
	color: #324b4e;
}
</style>
</head>
<body id="deal-list" data-com="pagecommon" data-page-id="100265"
	data-iswebview="false">
	<div>
		<img src="../staticfile/images/wechatfocus.jpg" style="width: 100%;" />
	</div>
	<br />
	<div class="deal-container">
		<div id="deals" class="deals-list">
			<!-- 每个商家的开始符 -->
			<c:forEach items="${mchUsers}" var="mchUser">
				<dl class="list" gaevent="common/poilist">
					<dd class="poi-list-item">
						<a class="react" href="tosinglemch?mchId=${mchUser.mchUser.mchId}"
							data-ctpoi="792766175882313728_a73227298_c5_e2978301047565630733">
							<span class="poiname"
							style="font-size: 22px; font-family: Microsoft YaHei"><b>${mchUser.mchUser.shopName}</b></span>
							<div class="kv-line-r">
								<h6>
									<span class="stars" style="color: #E52010"><em
										class="star-text"> <c:forEach var="i" begin="1"
												end="${mchUser.mchUser.points}" step="1">
												<i class="text-icon icon-star"></i>
											</c:forEach>${mchUser.mchUser.points}星</em></span>
								</h6>
							</div>
						</a>
					</dd>
					<dd>
						<dl class="list">
								<dd>
									<a href="tosinglemch?mchId=${mchUser.mchUser.mchId}"
										class="react ">
										<div class="dealcard dealcard-poi">
											<div
												style="background: transparent none repeat scroll 0% 0%;"
												class="dealcard-img imgbox">
												<img src="/haircutting/${mchUser.contentPictures[0]}"
													style="width: 100%;" />
											</div>
											<div class="dealcard-block-right">
												<div class="title text-block">${mchUser.mchUser.shopAddress}</div>
												<div class="price">
													<span class="strong" style="color: red">${mchUser.mchUser.serviceStatus}</span>
												<span
														class="tag"
														style="color: #fff; border-color: #E52010; background-color: #E52010;">进入取号</span>
												</div>
											</div>
										</div>
									</a>
									<p class="posi-right-bottom">
										<a class="statusInfo" onclick="return false;" href="#">累计排队<span
											class="strong">${mchUser.mchUser.totalSaleCount}次</span></a>
									</p>
								</dd>

						</dl>
					</dd>
				</dl>
				<!-- 每个商家的结束符 -->
			</c:forEach>
			<dl id="listlast" gaevent="common/poilist"></dl>
		</div>
	</div>

	<script type="text/javascript">
  
	</script>
</body>
</html>