function queryRedeem() {
	var query = document.getElementById("query").value;
	if (query.length == 0) {
		return false;
	}
	document.getElementById("querya").href="/haircutting/mchuser/toredeemcodelist?query="+query;
}