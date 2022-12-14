<%@ page language="java" import="java.util.*" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jstl/core_rt" prefix="c"%>
<!DOCTYPE html>
<html>
<head>
<jsp:include page="./basic/back_js_css.jsp" />
</head>
<body>
	<jsp:include page="./basic/top.jsp" />
	<jsp:include page="./basic/menu_common.jsp" />
	<!-- main container -->
	<div class="content">
		<div class="container-fluid">
		
		   <!-- upper main stats -->
            <div id="main-stats">
                <div class="row-fluid stats-row">
                    <div class="span3 stat">
                        <div class="data">
                            <span class="number">2457</span>
                                                                           访问量
                        </div>
                        <span class="date">今日</span>
                    </div>
                    <div class="span3 stat">
                        <div class="data">
                            <span class="number">${totalMchCnt}</span>
                                                                            商家数
                        </div>
                        <span class="date">总计</span>
                    </div>
                    <div class="span3 stat">
                        <div class="data">
                            <span class="number">322</span>
                                                                               今日取号次数
                        </div>
                        <span class="date">今日</span>
                    </div>
                    <div class="span3 stat last">
                        <div class="data">
                            <span class="number">$2,340</span>
                                                                           历史取号次数
                        </div>
                        <span class="date">总计</span>
                    </div>
                </div>
            </div>
            <!-- end upper main stats -->
		
		
			<div id="pad-wrapper">
			    <!-- statistics chart built with jQuery Flot -->
				<div class="row-fluid chart"
					style="border-top: 1px solid #edeff1; padding-top: 10px;">
					<h4>本周流水</h4>
					<div class="span12">
						<div id="statsChart"></div>
					</div>
				</div>
				<!-- end statistics chart -->
			    <br/>
			    <br/>
			    <br/>
			    
			
				<!-- table sample -->
				<!-- the script for the toggle all checkboxes from header is located in js/theme.js -->
				<div class="table-products section">
					<div class="row-fluid head">
						<div class="span12">
							<h4>兑换码</h4>
						</div>
					</div>
					<div class="row-fluid filter-block">
						<div class="pull-right">
							<div class="ui-select">
								<select name="isused"
									onchange="select(this.options[this.options.selectedIndex].value)">
									<option value="all" >全部</option>
									<option value="used" >已兑换</option>
									<option value="notused" >未兑换</option>
								</select>
							</div>
							<input type="text" class="search" placeholder="请输入兑换码" id="query"
								onmouseout="queryRedeem()" /> <a id="querya" href=""
								class="btn-flat new-product">查询</a>
						</div>
					</div>
					<div class="row-fluid">
						<table class="table table-hover">
							<thead>
								<tr>
									<th class="span3">编号</th>
									<th class="span3"><span class="line"></span>兑换码</th>
									<th class="span3"><span class="line"></span>兑换商品名</th>
									<th class="span3"><span class="line"></span>金额</th>
									<th class="span3"><span class="line"></span>发送状态</th>
									<th class="span3"><span class="line"></span>是否兑换</th>
									<th class="span3"><span class="line"></span>操作</th>
								</tr>
							</thead>
							<tbody>
								<!-- row -->
								<!-- 遍历所有的兑换码 -->
								<c:forEach items="${cacheRedeemCodes}" var="cacheRredeemCode">
									<tr>
										<td>${cacheRredeemCode.redeemCode.codeId}</td>
										<td>${cacheRredeemCode.redeemCode.randomCode}</td>
										<td>${cacheRredeemCode.productName}</td>
										<td>${cacheRredeemCode.redeemCode.payMoney}</td>
										<c:if test="${cacheRredeemCode.redeemCode.isSendok == 1}">
										  <td><span class="label label-success">发送成功</span></td>
										</c:if>
										<c:if test="${cacheRredeemCode.redeemCode.isSendok == 0}">
										  <td>
										     <span class="label label-info">发送失败</span>
										  </td>
										</c:if>
										<c:if test="${cacheRredeemCode.redeemCode.isUsed==true}">
											<td><span class="label label-success">已兑换</span></td>
				
										</c:if>
										<c:if test="${cacheRredeemCode.redeemCode.isUsed==false}">
											<td><span class="label label-info">未兑换</span></td>
										</c:if>	
										  <td>
										    <c:if test="${cacheRredeemCode.redeemCode.isSendok == 0}">
										        <ul class="actions">
												    <li><i class="table-edit"
													style="background: url('../staticfile/images/ico-mail.png') no-repeat;"
													onclick="resend(${cacheRredeemCode.redeemCode.codeId})"></i><span style="color: green">重新发送</span></li>
											    </ul>
										    </c:if>	
										    <c:if test="${cacheRredeemCode.redeemCode.isSendok == 1}">
										        <c:if test="${cacheRredeemCode.redeemCode.isUsed == false}">
												  <ul class="actions">
													<li><i class="table-edit"
														onclick="redeem(${cacheRredeemCode.redeemCode.codeId})"></i><span
														style="color: green">兑换</span></li>
												  </ul>
											   </c:if>
											</c:if>
										 </td>
									</tr>
								</c:forEach>
							</tbody>
						</table>
					</div>
					<div class="pagination">
						<!-- 此处多传递一个参数来判断该查询请求来自主页 例如：&index=true -->
						<ul>
							<li><a
								href="<%=request.getContextPath()%>/mchuser/toredeemcodelist?currentPage=0&index=index">&#8249&#8249</a></li>
							<li><c:if test="${currentPage>0}">
									<a
										href="<%=request.getContextPath()%>/mchuser/toredeemcodelist?currentPage=${currentPage-1}&index=index">&#8249</a>
								</c:if></li>
							<li><c:if test="${currentPage<allPages}">
									<a
										href="<%=request.getContextPath()%>/mchuser/toredeemcodelist?currentPage=${currentPage+1}&index=index">&#8250</a>
								</c:if></li>
							<li><a
								href="<%=request.getContextPath()%>/mchuser/toredeemcodelist?currentPage=${allPages}&index=index">&#8250&#8250</a></li>
						</ul>
					</div>
				</div>
				<!-- end table sample -->
			</div>
		</div>
	</div>
	<div id="popDiv" class="mydiv" style="display: none;">
		<h1
			style="color: white; font-family: 黑体; margin-top: 10px; margin-bottom: 30px;">修改密码</h1>
		<div class="contact-form">
			<div class="signin">
				<div id="pwd1">
					<input id="oldpwd" type="password" placeholder="请输入原密码"
						onblur="validate(this)" />
				</div>
				<div id="wrongpwd"></div>
				<div id="pwd2">
					<input id="newpwd1" type="password"
						placeholder="请输入新密码(3-15位英文及数字)" />
				</div>
				<div id="pwd3">
					<input id="newpwd2" type="password" placeholder="请再次输入原密码" />
				</div>
				<div id="pwd4">
					<input id="edit" type="button" value="修改" onclick="subForm();" /><input
						id="cancel" type="button" onclick="closeDiv()" value="取消">
				</div>
			</div>
		</div>
	</div>
	<jsp:include page="./basic/pop.jsp" />
	<script type="text/javascript">
  $(function() {

	// jQuery Knobs
	$(".knob").knob();

	// jQuery UI Sliders
	$(".slider-sample1").slider({
		value : 100,
		min : 1,
		max : 500
	});
	$(".slider-sample2").slider({
		range : "min",
		value : 130,
		min : 1,
		max : 500
	});
	$(".slider-sample3").slider({
		range : true,
		min : 0,
		max : 500,
		values : [ 40, 170 ],
	});

	// jQuery Flot Chart
	var visitors = [ [ 1, ${reports[6].value} ], [ 2, ${reports[5].value} ], [ 3, ${reports[4].value} ], [ 4, ${reports[3].value} ],
			[ 5, ${reports[2].value} ], [ 6, ${reports[1].value} ], [ 7, ${reports[0].value} ] ];

	var plot = $.plot($("#statsChart"), [ {
		data : visitors,
		label : "流水"
	} ],
			{
				series : {
					lines : {
						show : true,
						lineWidth : 1,
						fill : true,
						fillColor : {
							colors : [ {
								opacity : 0.1
							}, {
								opacity : 0.13
							} ]
						}
					},
					points : {
						show : true,
						lineWidth : 2,
						radius : 3
					},
					shadowSize : 0,
					stack : true
				},
				grid : {
					hoverable : true,
					clickable : true,
					tickColor : "#f9f9f9",
					borderWidth : 0
				},
				legend : {
					// show: false
					labelBoxBorderColor : "#fff"
				},
				colors : ["#30a0eb" ],
				xaxis : {
					ticks : [ [ 1, '${reports[6].date}' ], [ 2, '${reports[5].date}' ], [ 3, '${reports[4].date}' ],
							[ 4, '${reports[3].date}' ], [ 5, '${reports[2].date}'], [ 6, '${reports[1].date}'],
							[ 7, '${reports[0].date}'] ],
					font : {
						size : 12,
						family : "Open Sans, Arial",
						variant : "small-caps",
						color : "#697695"
					}
				},
				yaxis : {
					ticks : 3,
					tickDecimals : 0,
					font : {
						size : 12,
						color : "#9da3a9"
					}
				}
			});

	function showTooltip(x, y, contents) {
		$('<div id="tooltip">' + contents + '</div>').css({
			position : 'absolute',
			display : 'none',
			top : y - 30,
			left : x - 50,
			color : "#fff",
			padding : '2px 5px',
			'border-radius' : '6px',
			'background-color' : '#000',
			opacity : 0.80
		}).appendTo("body").fadeIn(200);
	}

	var previousPoint = null;
	$("#statsChart")
			.bind(
					"plothover",
					function(event, pos, item) {
						if (item) {
							if (previousPoint != item.dataIndex) {
								previousPoint = item.dataIndex;

								$("#tooltip").remove();
								var x = item.datapoint[0].toFixed(0), y = item.datapoint[1]
										.toFixed(0);

								var month = item.series.xaxis.ticks[item.dataIndex].label;

								showTooltip(item.pageX, item.pageY,
										month + item.series.label
												+ ": " + y + "元");
							}
						} else {
							$("#tooltip").remove();
							previousPoint = null;
						}
					});
});</script>
	<script type='text/javascript'>  
    function select(s){  
    	 window.location.href="/haircutting/mchuser/toredeemcodelist?isused="+s; 
    };
	
    /*
	    置换兑换码
	*/
	function redeem(id)
	{
	   if(id == "")
	   {
	     alert("必须选择一个兑换码");
	     return ;
	   }
	
	  //兑换
	  $.post("<%=request.getContextPath()%>/mchuser/changeredeemcode",{redeemcode:id},function(data){
	   if (data.isResultOk) 
	   {
	     alert("操作成功");
	     $('#'+id).text("已兑换");
	     $('#'+id).css("color","green");
	    } else {
	     alert(date.resultMsg);
	    }
	  });
	
	   return;
	 };
	/*
	   给微信用户重新发送兑换码
	*/
	function resend(id)
	{
	   if(id == "")
	   {
	     alert("必须选择一个兑换码");
	     return ;
	   }
	
	  //兑换
	  $.post("<%=request.getContextPath()%>/mchuser/resendredeemcode",{redeemcode:id},function(data){
	   if (data.isResultOk) 
	   {
	     alert("操作成功");
	     $('#'+id).text("已兑换");
	     $('#'+id).css("color","green");
	    } else {
	     alert(data.resultMsg);
	    }
	  });
	
	   return;
	 };
</script>
</body>
</html>