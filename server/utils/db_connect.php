<?php
/**
 * Function to connect with database
 */
function db_connect() {
	// import database connection variables
	require_once __DIR__ . '/db_config.php';

	// Connecting to mysql database
	$mysqli = new mysqli(DB_SERVER, DB_USER, DB_PASSWORD, DB_DATABASE);
	if ($mysqli->connect_errno) {
		echo "Failed to connect to MySQL: (" . $mysqli->connect_errno . ") " . $mysqli->connect_error;
	}
/*
	if ($_POST["debug"])
		echo $mysqli->host_info . "<br>";
*/
	// return mysql instance
	return $mysqli;
}

function db_select($query) {
	$response = array();
	$db = db_connect();
	if ($_POST["debug"])
		echo "$query<br>";
	if (($result = $db->query($query)) != null) {
		while ($row = $result->fetch_assoc()) {
			array_push($response,$row);		
		}
	} else
		$response = "$db->error ($db->errno)";
	$db->close();
	return $response;
}

function db_insert($query) {
	$db = db_connect();
	if ($_POST["debug"])
		echo "$query<br>";
	if ($db->query($query))
		$response = $db->affected_rows;
	else
		$response = "$db->error ($db->errno)";
	$db->close();
	return $response;
}

function db_delete($query) {
	$db = db_connect();
	if ($_POST["debug"])
		echo "$query<br>";
	if ($db->query($query))
		$response = $db->affected_rows;
	else
		$response = "$db->error ($db->errno)";
	$db->close();
	return $response;
}

function db_update($query) {
	$db = db_connect();
	if ($_POST["debug"])
		echo "$query<br>";
	if ($db->query($query))
		$response = $db->affected_rows;
	else
		$response = "$db->error ($db->errno)";
	$db->close();
	return $response;
}
?>