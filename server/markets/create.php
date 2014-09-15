<?php
/*
 * Create market info.
*/
require_once __DIR__ . '/../utils/simple_html_dom.php'; // for parsing html
require_once __DIR__ . '/../utils/db_connect.php'; // db interface
require_once __DIR__ . '/../utils/sdk.php'; // maps android version (e.g., 2.2) to api level (e.g., 7)

$params = array("market", "package", "category", "rating", "votes", "downloads", "price", "author", "datePublished", "url");
$table = "markets";
$response = array();

date_default_timezone_set('UTC'); // have to set this or get warnings when manipulating dates

if ($_POST["debug"])
	echo "<br>";
main();

function main() {
	global $response, $package;
	if (isset($_POST["package"])) {
		$package = $_POST["package"];

		if (!isset($_POST["name"]) && !isset($_POST["versionLabel"])) {
			// get package name and version number from packages table so we can find the app in markets
			$query = "SELECT `name`, `versionLabel`, `size` FROM `packages` WHERE `checksum` = '$package';";
			$result = db_select($query);
			$name = $result[0]["name"];
			$versionLabel = $result[0]["versionLabel"];

			// if name couldn't be retrieved, fail
			if (!$name) {
				$response["success"] = 0;
				$response["message"] = "package not found";
				echo json_encode($response);
				exit(-1);
			}
		} else {
			$versionLabel = $_POST["versionLabel"];
			$name = $_POST["name"];
		}
		// scrape app details from market pages
		// skip markets where entries already exist
		$query = "SELECT `market` FROM `markets` WHERE `package` = '$package';";
		$result = db_select($query);
		$i = 0;
		if (count($result) > 0) {
			for($i = 0; $i < count($result); $i++)
				$markets[$i] = $result[$i]["market"];
			if (!in_array("Google Play", $markets)) {
				try {
					google($name, $versionLabel);
				} catch (Exception $e) {

				}
			} else {
				echo "<span class=\"success\">Google Play entry exists</span><br>";
				$response["success"]["Google Play"] = 0;
				$response["message"]["Google Play"] = "market entry exists";
			}
			if (!in_array("AppBrain", $markets)) {
				try {
					appbrain($name, $versionLabel);
				} catch (Exception $e) {

				}
			} else {
				echo "<span class=\"success\">AppBrain entry exists</span><br>";
				$response["success"]["AppBrain"] = 0;
				$response["message"]["AppBrain"] = "market entry exists";
			}
			if (!in_array("Amazon Appstore", $markets)) {
				try {
					amazon($name, $versionLabel);
				} catch (Exception $e) {

				}
			} else {
				echo "<span class=\"success\">Amazon Appstore entry exists</span><br>";
				$response["success"]["Amazon Appstore"] = 0;
				$response["message"]["Amazon Appstore"] = "market entry exists";
			}
		} else {
			google($name, $versionLabel);
			appbrain($name, $versionLabel);
			amazon($name, $versionLabel);
		}
	} else {
		$response["success"] = 0;
		$response["message"] = "Required field(s) missing";
	}

	// response
	echo json_encode($response);
}

/**
 * Scrapes Google Play for app details.
 * @param unknown $name
 * @param unknown $versionLabel
 */
function google($name, $versionLabel) {
	if ($_POST["debug"])
		echo "<b>Scraping Google Play...</b><br>";
	global $market, $package, $category, $rating, $votes, $downloads, $price, $author, $datePublished, $url, $response;
	// google play
	$market = "Google Play";
	$url = "https://play.google.com/store/apps/details?id=$name";
	if ($_POST["debug"])
		echo "<a href=\"$url\" target=\"blank\">$url</a><br>";
	if (url_exists($url) && ($html = file_get_html($url)) != null) {
		$metadata = $html->find(".doc-metadata-list",0);

		$version = $metadata->find("dd[itemprop=softwareVersion]",0)->innertext;
		// if there are multiple versions, get the version number from the most recent review
		if ($version == "Varies with device") {
			$version = $html->find(".review-body-column",0)->innertext;
			$version = explode("ersion ", $version);
			$version = explode('<', $version[1]);
			$version = $version[0];
		}

		// if version doesn't match, market data may not be valid
		if ($versionLabel == $version) {
			// dunno why they didn't do a category itemprop so we have to look for the link that starts with /store/apps/category
			$category = $metadata->find("a[href^=/store/apps/category/]",0)->innertext;
			$category = str_replace("&amp;","&",$category);
				
			// retarded workaround for dom parser's unexplainable failure to get rating
			$rating = explode(' ', $metadata->find("dd[itemprop=aggregateRating]",0)->find("div",0)->title);
			$rating = $rating[1];

			$votes = $metadata->find("span[itemprop=ratingCount]",0)->content;

			// downloads returns as "500,000 - 1,000,000", etc. so convert that to an int using the mean
			$downloads = $metadata->find("dd[itemprop=numDownloads]",0)->innertext;
			// split into the low end and high end of the range, ignore the dash
			$split = explode(' ',$downloads);
			// take out the stupid commas
			$low = str_replace(',', '', $split[0]);
			$high = str_replace(',', '', $split[2]);
			// get the average
			$downloads = (($low+$high)/2);

			$price = $metadata->find("span[itemprop=price]",0)->content;
			$price = str_replace("$","",$price);

			$author = $metadata->find("span[itemprop=author]",0)->find("span[itemprop=name]",0)->content;

			// convert date from  March 25, 2013 to 2013-03-25 00:00:00
			$datePublished = date('Y-m-d',strtotime($metadata->find("time[itemprop=datePublished]",0)->innertext));

			// get min sdk & update packages table
			$minSdk = explode(' ',$html->find("dd",3)->innertext);
			$minSdk = getApiLevel($minSdk[0]);
			$query = "UPDATE `packages` SET `minSdk` = '$minSdk' WHERE `checksum` = '$package';";
			$db = db_connect();
			$db->query($query);

			// insert market entry
			create();
		} else {
			$response["success"][$market] = 0;
			$response["message"][$market] = "version mismatch ($version != $versionLabel)";
		}
	} else {
		$response["success"][$market] = 0;
		$response["message"][$market] = "app unavailable";
	}
}

/**
 * Scrapes AppBrain for app details.
 * @param unknown $name
 * @param unknown $versionLabel
 */
function appbrain($name, $versionLabel) {
	if ($_POST["debug"])
		echo "<b>Scraping AppBrain...</b><br>";
	global $market, $package, $category, $rating, $votes, $downloads, $price, $author, $datePublished, $url, $response;
	// appbrain
	$market = "AppBrain";
	$url = "http://www.appbrain.com/app/$name";
	if ($_POST["debug"])
		echo "<a href=\"$url\" target=\"blank\">$url</a><br>";
	if (url_exists($url) && ($html = file_get_html($url)) != null) {

		// get version from changelog
		$i = 0;
		while (method_exists($html->find(".clEntry",$i),"find") && $html->find(".clEntry",$i)->find(".clUpdate") == null && $html->find(".clEntry",$i)->find(".clNew") == null)
			$i++;
		if (isset($html->find(".clDesc",$i)->innertext)) {
			$version = explode(' ',$html->find(".clDesc",$i)->innertext);
			$version = $version[1];

			// check version match or if it's a new app w/o a version number, what the heck add it
			if ($versionLabel == $version || $html->find(".clEntry",$i)->find(".clNew") != null) {
				$category = $html->find("span[itemprop=title]",1)->innertext;
				$category = str_replace("&amp;","&",$category);

				$author = $html->find("span[itemprop=author]",0)->innertext;

				// the ratings block looks like 6547285 (3.62 average) so split that up & trim
				$votes = $html->find(".appPageCell",4)->find("span",1)->innertext . "<br/>";
				$votes = explode(' ',$votes);
				// take off the parens off the rating
				$rating = str_replace(')','',str_replace('(','',$votes[1]));
				$votes = $votes[0];

				$price = $html->find("span[class^=app-price]",0)->innertext;
				if ($price == "Free")
					$price = 0;
				else
					// take off the dollar sign
					$price = str_replace('$','',$price);

				// convert date from  March 25, 2013 to 2013-03-25 00:00:00
				$datePublished = date('Y-m-d',strtotime($html->find(".clTime",0)->innertext));

				// numdownloads returns as 50,000+, etc. so remove + and commas
				$downloads = str_replace('+','',$html->find("span[itemprop=numdownloads]",0)->innertext);
				$downloads = str_replace(',','',$downloads);

				// appbrain uses "1576 kb" format
				$size = $html->find("span[itemprop=filesize]",0)->innertext;
				if (substr($size,strlen($size)-2,strlen($size)-1) == "kb")
					$size = intval(substr($size,0,-2)) * 1024;

				create();
			} else {
				$response["success"][$market] = 0;
				$response["message"][$market] = "version mismatch ($version != $versionLabel)";
			}
		} else {
			$response["success"][$market] = 0;
			$response["message"][$market] = "app unavailable";
		}
	} else {
		$response["success"][$market] = 0;
		$response["message"][$market] = "app unavailable";
	}
}

/**
 * Scrapes Amazon Appstore for app details.
 * @param unknown $name
 * @param unknown $versionLabel
 */
function amazon($name, $versionLabel) {
	if ($_POST["debug"])
		echo "<b>Scraping Amazon...</b><br>";
	global $market, $package, $category, $rating, $votes, $downloads, $price, $author, $datePublished, $url, $response;
	// amazon
	$market = "Amazon Appstore";
	// amazon doesn't make it easy by giving us a url using the package name
	// so we do a search for the package name & use the first result
	$url = "http://www.amazon.com/s/ref=nb_sb_noss?field-keywords=".$name; // amazon search
	if ($_POST["debug"])
		echo "<a href=\"$url\" target=\"_blank\">$url</a><br>Checking if first search result might be an app... ";
	$html = file_get_html($url); // get the search result
	if ($html->find(".newaps") != null) {
		$url = $html->find(".newaps",0)->find("a",0)->href; // get the url from the first result
		// take out the junk in the url
		$url = explode('/', $url);
		$url = implode('/', array($url[0], $url[1], $url[2], $url[4], $url[5]));
		if ($_POST["debug"])
			echo "<a href=\"$url\" target=\"blank\">" . $url . "</a><br>";

		if (($html = file_get_html($url)) !== null && ($techspecs = $html->find(".bucket",7)) !== null) { // actual app page

			// check version match
			$version = $techspecs->find("li",1)->innertext;
			$version = explode(" ",$version);
			$version = $version[1];

			// get date as a double check that it's an app
			$datePublished = explode('</b>', $html->find(".bucket",3)->find("li",1)->innertext);
			$datePublished = $datePublished[1];
			if ($datePublished != null && $version != null) {
				if ($versionLabel == $version) {

					$author = $html->find(".buying",2)->find("span",1)->find("a",0)->innertext;

					// convert date from  March 25, 2013 to 2013-03-25 00:00:00
					$datePublished = date('Y-m-d',strtotime($datePublished));

					$size = $techspecs->find("li",0)->innertext;
					$size = explode(" ",$size);
					$size = $size[1];
					if ($size == 'M')
						$size = substr($size, 0, -2);
					$price = $html->find("b.priceLarge",0)->innertext;
					// take out the dollar sign
					$price = str_replace('$','',$price);

					$rating = explode(' ', $html->find("span[title$=out of 5 stars]",0)->title);
					$rating = $rating[0];

					$votes = explode(' ', $html->find(".crAvgStars",0)->find("a",2)->innertext);
					$votes = str_replace(',','',$votes[0]);

					$category = $html->find(".bucket",8)->find("li",0)->find("a",1)->innertext;
					$category = str_replace("&amp;","&",$category);
					create();
				} else {
					$response["success"][$market] = 0;
					$response["message"][$market] = "version mismatch ($version != $versionLabel)";
				}
			} else {
				$response["success"][$market] = 0;
				$response["message"][$market] = "app unavailable";
			}
		} else {
			$response["success"][$market] = 0;
			$response["message"][$market] = "app unavailable";
		}
	} else {
		if ($_POST["debug"])
			echo "<span class=\"fail\">nope. chuck testa.</span><br>";
		$response["success"][$market] = 0;
		$response["message"][$market] = "app unavailable";
	}
}

function create() {
	global $market, $package, $category, $rating, $votes, $downloads, $price, $author, $datePublished, $url, $response;
	// insert market info
	$query = "INSERT INTO `markets` (`market`, `package`, `category`, `rating`, `votes`, `downloads`, `price`, `author`, `datePublished`, `url`) VALUES ('$market', '$package', '$category', '$rating', '$votes', '$downloads', '$price', '$author', '$datePublished', '$url');";
	$result = db_insert($query);
	if ($result == 1) {
		$response["success"][$market] = 1; // success
		$response["message"][$market] = "market entry created";
	} else {
		$response["success"][$market] = 0; // fail
		$response["message"][$market] = $result;
	}
}

function url_exists($url) {
	if ($_POST["debug"])
		echo "Checking if url exists... ";
	$file_headers = @get_headers($url);
	//if (strpos($file_headers[0],"404")) {
	if (@file_get_contents($url) == null) {
		if ($_POST["debug"])
			echo "<span class=\"fail\">url doesn't exist: $file_headers[0]</span><br>";
		return false;
	}
	if ($_POST["debug"])
		echo "<span class=\"success\">url exists: $file_headers[0] </span><br>";
	return true;
}
?>