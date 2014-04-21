<?php 
require_once __DIR__ . '/../utils/db_connect.php'; // db interface

$src = "https://nmtsfs.org/jwernicke/";
$dst = "https://localhost/~james/";

// get packages from remote server
$url = $src . "heracles/packages/request.php";
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

// send packges to destination server

?>