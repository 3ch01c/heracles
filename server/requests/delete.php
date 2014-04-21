<?php
/*
 * Delete package(s).
*/
require_once __DIR__ . "/../utils/db_connect.php"; // db interface

$params = array("package", "permission");
$table = "requests";
$response = array();

// base query
$query = "DELETE FROM `$table` WHERE ";

// predicates
$and = false; // true if need to add 'AND'
foreach ($params as $param) {
	if (isset($_POST[$param])) {
		// insert AND if necessary
		if ($and)
			$query = "$query AND";
		$query = "$query `$param` = '$_POST[$param]'";
		$and = true;
	}
}

// if no predicates, delete everything
if (!$and)
	$query = "DELETE FROM `$table`";

$query = $query . ';';

// display query
if ($_POST["debug"])
	echo "$query<br>";

// execute query
$db = db_connect();
if ($result = $db->query($query)) {
	$response["success"] = 1; // success
	$response["message"] = "$db->affected_rows request(s) deleted";
} else {
	$response["success"] = 0; // fail
	$response["message"] = "$db->error ( $db->errno )";
}
$db->close();

// response
echo json_encode($response);
?>