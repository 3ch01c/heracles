<?php
/*
 * Update package(s).
 */
require_once __DIR__ . "/../utils/db_connect.php"; // db interface

// TODO: update on more than checksum
$from_params = array("checksum");
$params = array("checksum", "name", "label", "versionCode", "versionLabel", "minSdk", "targetSdk", "size");
$table = "packages";
$response = array();

// base query
$query = "UPDATE `$table` SET ";

// predicates
$and = false; // true if need to add 'AND'
foreach ($params as $param) {
	if (isset($_POST[$param])) {
		// insert AND if necessary
		if ($and)
			$query = "$query, ";
		$query = "$query`$param` = '$_POST[$param]'";
		$and = true;
	}
}

// if no params set, fail
if (!$and) {
	$response["success"] = 0;
	$response["message"] = "nothing to update";
	echo json_encode($response);
	exit;
}

$query = $query . " WHERE ";

$and = false;
foreach ($from_params as $param) {
	if (isset($_POST[$param])) {
		// insert AND if necessary
		if ($and)
			$query = "$query AND";
		$query = "$query `$param` = '$_POST[$param]'";
		$and = true;
	}
}
	
$query = $query . ";";

// display query
if ($_POST["debug"])
	echo "$query<br>";

// execute query
$db = db_connect();
if ($result = $db->query($query)) {
	$response["success"] = 1; // success
	$response["message"] = $db->affected_rows . " package(s) updated";
} else {
	$response["success"] = 0; // fail
	$response["message"] = $db->error . '(' . $db->errno . ')';
}
$db->close();

// response
echo json_encode($response);
?>