<html>
<head>
<link rel="stylesheet" type="text/css" href="../css/default.css"
	media="screen" />
</head>
</html>
<?php 
/*
 * Gets market info for all packages missing it.
*/
require_once __DIR__ . '/../utils/db_connect.php'; // db interface

$response = array();
date_default_timezone_set('UTC'); // have to set this or get warnings when manipulating dates
$start = time();
ob_start();

//$url = "https://127.0.0.1/~james/heracles/markets/create.php";
//$url = "https://127.0.0.1/jwernicke/heracles/markets/create.php";

// get list of package checksums
/*
$query = "SELECT `checksum`,`name` FROM `packages`;";
$db = db_connect();
$result = $db->query($query);
$i = 0;
$packages = array();
while ($row = $result->fetch_assoc()) {
	$packages[$i] = $row["checksum"];
	$names[$i] = $row["name"];
	$i++;
}
*/
$url = "https://localhost/~james/heracles/packages/request.php";
$ch = curl_init();
curl_setopt($ch, CURLOPT_URL, $url);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_POST, true);
curl_setopt($ch, CURLOPT_POSTFIELDS, null);
curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);

$result = curl_exec($ch);
//echo $result;
$packages = array();
$packages = json_decode($result,true);
//echo $packages;
$packages = $packages["packages"];
curl_close($ch);

$url = "http://127.0.0.1/~james/heracles/markets/create.php";

// set up market arrays
$markets = array("Google Play", "AppBrain", "Amazon Appstore");
foreach ($markets as $market)
	$response[$market] = array();
$total_updates = 0;
$max = count($packages);
// get market info for each package
for ($i = 0; $i < $max; $i++) {
	set_time_limit(2000);
	$params["package"] = $packages[$i]["checksum"];
	$params["name"] = $packages[$i]["name"];
	$params["versionLabel"] = $packages[$i]["versionLabel"];
	$params["debug"] = true;
	$elapsed = time_elapsed(time()-$start);
	$eta = time_elapsed(((time()-$start)/($i+1))*($max-$i+1));
	echo $i+1 . "/" . $max . " time to complete:" . $eta . " (" . $elapsed . " elapsed) " . " Fetching market data for " . $packages[$i]["name"] . "...";
	echo str_repeat(" ", 1024), "\n";
	ob_flush();
	flush();

	// send request to markets/create.php
	$ch = curl_init();
	curl_setopt($ch, CURLOPT_URL, $url);
	curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
	//curl_setopt($ch, CURLOPT_FOLLOWLOCATION, true);
	curl_setopt($ch, CURLOPT_POST, true);
	curl_setopt($ch, CURLOPT_POSTFIELDS, $params);
	curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);

	$result = curl_exec($ch);
	/*
	 $data = http_build_query($params);
	$options = array(
			"http" => array(
					"method" => "POST",
					"header" => "Content-Type: application/x-www-form-urlencoded",
					"content" => $data
			)
	);
	$context = stream_context_create( $options );
	$result = file_get_contents( $url, false, $context );
	echo $result . "<br>";
	*/
	echo "$result<hr>";
	$result = json_decode($result, true);
	ob_flush();
	flush();
	$updates = 0;
	foreach ($markets as $market) {
		if ($result["success"][$market] != 0) {
			array_push($response[$market],$params["package"]);
			$updates++;
			$total_updates += $updates;
		}
	}
	curl_close($ch);
}
$response["success"] = 1;
$response["message"] = "$total_updates market entries created";
echo json_encode($response);

function time_elapsed($secs) {
	$bit = array(
			'y' => $secs / 31556926 % 12,
			'w' => $secs / 604800 % 52,
			'd' => $secs / 86400 % 7,
			'h' => $secs / 3600 % 24,
			'm' => $secs / 60 % 60,
			's' => $secs % 60
	);

	foreach($bit as $k => $v)
		if($v > 0)$ret[] = $v . $k;

	if ($ret == null)
		return "0s";
	return implode(' ', $ret);
}
?>