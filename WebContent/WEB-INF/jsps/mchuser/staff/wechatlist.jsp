<%@ page language="java" import="java.util.*" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jstl/core_rt" prefix="c"%>
<!DOCTYPE html>
<html>
<head>
<!-- TODO :抽出公共  top.jsp 界面 -->
<jsp:include page="../basic/back_js_css.jsp" />
<script type="text/javascript" src="../staticfile/commonjs/uptosale.js"></script>
</head>
<body>
	<jsp:include page="../basic/top.jsp" />
	<!-- sidebar -->
	<jsp:include page="../basic/menu_common.jsp" />
	<!-- end sidebar  class="btn btn-primary" -->
	<!-- main container -->
	<div class="content">
		<div class="container-fluid">
			<div id="pad-wrapper">
				<!-- table sample -->
				<!-- the script for the toggle all checkboxes from header is located in js/theme.js -->
				<div class="table-products section">
					<div class="row-fluid head">
						<div class="span12">
							<h4>共检索到<font color="red">${allPages}</font>个关注</h4>
						</div>
					</div>
					<div class="row-fluid">
						<table class="table table-hover">
							<thead>
								<tr>
									<th class="span3">微信昵称</th>
									<th class="span3"><span class="line"></span>性别</th>
									<th class="span3"><span class="line"></span>手机号</th>
									<th class="span3"><span class="line"></span>头像</th>
									<th class="span3"><span class="line"></span>微信openId</th>
									<th class="span3"><span class="line"></span>关注时间</th>
								</tr>
							</thead>
							<tbody>
								<!-- row -->
								<c:forEach items="${cacheWechat}" var="wechat">
									<tr>
										<td >${wechat.wechatUser.userNickName}</td>
									    <td >${wechat.wechatUser.userSex}</td>
										<td>${wechat.wechatUser.userTelNum}</td>
										<td>
										   <img src="${wechat.wechatUser.userHeadPath}"/>
										</td>
										<td>${wechat.wechatUser.userOpenid}</td>
									    <td>${wechat.relation.followTime}</td>
									</tr>
									<!-- 商家下面的所有商品 列表 开始 符号 -->
								</c:forEach>
							</tbody>
						</table>
					</div>
					<div class="pagination">
						<ul>
							<li><a
								href="<%=request.getContextPath()%>/mchuser/tostafflist?currentPage=0">&#8249&#8249</a></li>
							<li><c:if test="${currentPage>0}">
									<a
										href="<%=request.getContextPath()%>/mchuser/tostafflist?currentPage=${currentPage-1}">&#8249</a>
								</c:if></li>
							<li><c:if test="${currentPage<allPages}">
									<a
										href="<%=request.getContextPath()%>/mchuser/tostafflist?currentPage=${currentPage+1}">&#8250</a>
								</c:if></li>
							<li><a
								href="<%=request.getContextPath()%>/mchuser/tostafflist?currentPage=${allPages}">&#8250&#8250</a></li>
						</ul>
					</div>
				</div>
				<!-- end table sample -->
			</div>
		</div>
	</div>
	<jsp:include page="../basic/pop.jsp" />
	
	 <script type="text/javascript">

    </script>
</body>
</html>