<?php
/*
 * Create device.
*/
require_once __DIR__ . "/../utils/db_connect.php"; // db interface

$params = array("uuid", "package", "permission", "active");
$table = "device_profiles";
$response = array();

if (isset($_POST["uuid"]) && isset($_POST["package"]) && isset($_POST["permission"])) {

	// insert query
	$query = "INSERT INTO `$table` (";

	// column names
	$and = false;
	foreach ($params as $param) {
		if (isset($_POST[$param])) {
			if ($and)
				$query = "$query, ";
			$query = "$query`$param`";
			$and = true;
		}
	}
	$query = "$query) VALUES (";

	// column values
	$and = false;
	foreach ($params as $param) {
		if (isset($_POST[$param])) {
			if ($and)
				$query = "$query, ";
			$query = "$query'$_POST[$param]'";
			$and = true;
		}
	}
	$query = "$query);";

	if ($_POST["debug"])
		// display query
		echo "$query<br>";

	// execute query
	$db = db_connect();
	if ($db->query($query)) {
		$response["success"] = 1; // success
		$response["message"] = "device profile created";
	} elseif ($db->errno == 1062) {
		$response["success"] = 0; // fail
		$response["message"] = "device profile exists";
	} else {
		$response["success"] = 0; // fail
		$response["message"] = "$db->error ($db->errno)";
	}
	$db->close();

} else {
	$response["success"] = 0;
	$response["message"] = "Required field(s) missing";
}

// response
echo json_encode($response);
?>