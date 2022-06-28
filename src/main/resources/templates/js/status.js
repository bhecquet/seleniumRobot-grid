function displayNodeStatus(nodeUrl) {
	var nodeStatusElement = document.getElementById("nodeStatus");
	nodeStatusElement.innerHTML =  '<iframe src="' + nodeUrl + '/extra/NodeStatusServlet" width=50% height=600px"></iframe>'
}