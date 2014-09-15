<?php
/*
 * Request user(s).
*/
require_once __DIR__ . "/../utils/db_connect.php"; // db interface

$params = array("name");
$table = "users";
$response = array();

// base query
$query = "SELECT * FROM `$table` WHERE";

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

// if no predicates, select everything
if (!$and)
	$query = "SELECT * FROM `$table`";

$query = $query . ';';

// display query
if ($_POST["debug"])
	echo "$query<br>";

// execute query
$db = db_connect();
if ($result = $db->query($query)) {
	$response["success"] = 1; // success
	$response["message"] = "$db->affected_rows user(s) found";
	
	// add results to response
	$result->data_seek(0);
	$response["users"] = array();
	while ($row = $result->fetch_assoc()) {
		$user = array();
		foreach ($params as $param)
			if (isset($row[$param]))
				$user[$param] = $row[$param];
		array_push($response["users"], $user);
	}
} else {
	$response["success"] = 0; // fail
	$response["message"] = $db->error . '(' . $db->errno . ')';
}
$db->close();

// response
echo json_encode($response);
?>