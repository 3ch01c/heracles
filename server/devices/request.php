<?php
/*
 * Request packages.
*/

$params = array("uuid", "make", "model", "carrier", "rom", "version", "user");
$table = "devices";
$response = array();

// base query
$query = "SELECT * FROM `$table` WHERE ";
$and = false; // append "AND" to query when compound predicate

// build predicates from post params
foreach ($params as $param)
	add_predicate($param);

// if no post params, then select everything
if (!$and)
	$query = "SELECT * FROM `$table`";

// terminate query with semicolon
$query = $query . ';';

// display query
if ($_POST["debug"])
	echo $query . "<br>";

// import database connection variables
require_once __DIR__ . '/../utils/db_connect.php';

// connect to db
$db = db_connect();

// execute query
if ($result = $db->query($query)) {
	$response["success"] = 1; // success
	$response["message"] = $db->affected_rows . " package(s) found";

	// add results to response
	$result->data_seek(0);
	$response["$table"] = array();
	while ($row = $result->fetch_assoc()) {
		$market = array();
		foreach ($params as $param)
			if (isset($row[$param]))
				$market[$param] = $row[$param];

		array_push($response["$table"], $market);
	}
} else {
	$response["success"] = 0; // fail
	$response["message"] = $db->error . '(' . $db->errno . ')';
}

// TODO: make this part of a destructor function on a db class
$db->close();

// response
echo json_encode($response);

function add_predicate($param) {
	global $query, $and;
	if (isset($_POST[$param])) {
		// insert AND if necessary
		if ($and)
			$query = $query . " AND";
		$query = $query . " `". $param . "`='" . $_POST[$param] . "'";
		$and = true;
	}
}
?>