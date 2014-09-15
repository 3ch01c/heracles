<?php 
require_once __DIR__ . '/../utils/simple_html_dom.php'; // for parsing html
require_once __DIR__ . '/../utils/qp.php'; // for parsing html better?
date_default_timezone_set('UTC');
$name = "com.sec.pcw";
echo "<b>Google Play</b><br>";
$url = "http://play.google.com/store/apps/details?id=$name";
echo "<a href=\"$url\" target=\"_blank\">$url</a><br>";
// market page
$html = htmlqp($url);
// version
if (($version = $html->top("[itemprop=softwareVersion]")->text()) === "Varies with device") {
	// if multiple versions, get version number from latest review
	$version = $html->top(".review-body-column")->innerHTML();
	$version = explode("ersion ", $version);
	$version = explode('<', $version[1]);
	$version = $version[0];
}
echo "version=$version<br>";
// category
foreach ($html->top("a[href]") as $item)
	if (strpos($item->attr("href"),"/store/apps/category") !== false)
	$category = $item->text();
echo "category=$category<br>";
// rating
$rating = $html->top("[itemprop=ratingValue]")->attr("content");
echo "rating=$rating<br>";
// votes
$votes = $html->top("[itemprop=ratingCount]")->attr("content");
echo "votes=$votes<br>";
// downloads
$downloads = $html->top("[itemprop=numDownloads]")->text();
$downloads = explode(' ',$downloads);
$low = str_replace(',', '', $downloads[0]);
$high = str_replace(',', '', $downloads[2]);
$downloads = (($low+$high)/2);
echo "downloads=$downloads<br>";
$author = $html->top("[itemprop=author]")->find("[itemprop=name]")->attr("content");
echo "author=$author<br>";
$price = $html->top("[itemprop=price]")->attr("content");
echo "price=$price<br>";
if (($datePublished = $html->top("[itemprop=datePublished]")->text()) != null)
	$datePublished = date('Y-m-d',strtotime($datePublished));
echo "published=$datePublished<br>";
if (($minSdk = $html->top("[itemprop=operatingSystems]")->next()->text()) === "Varies with device")
	$minSdk = null;
echo "minSdk=$minSdk<br>";





// AppBrain
echo "<b>AppBrain</b><br>";
$url = "http://www.appbrain.com/app/$name";
echo "<a href=\"$url\" target=\"_blank\">$url</a><br>";
$html = htmlqp($url);
if ($html->top(".clUpdate") == null) {
	$version = explode(' ',$html->top(".clNew")->parent()->next()->innerHTML());
	$version = $version[1];
	$datePublished = explode(' ',$html->top(".clNew")->parent()->prev()->text());
} else {
	$version = explode(' ',$html->top(".clUpdate")->parent()->next()->innerHTML());
	$version = $version[1];
	$datePublished = explode(' ',$html->top(".clUpdate")->parent()->prev()->text());
}
echo "version=$version<br>";
$category = $html->top("[itemprop=breadcrumb]")->lastChild()->text();
echo "category=$category<br>";
$author = $html->top("[itemprop=author]")->text();
echo "author=$author<br>";
$votes = $html->find(".app-bluebold")->next()->next()->innerHTML();
$votes = explode(' ',$votes);
$rating = $votes[1];
$rating = str_replace('(','',$rating);
$votes = $votes[0];
echo "votes=$votes<br>";
echo "rating=$rating<br>";
if (($price = $html->top("[class^=app-price]")->text()) === "Free")
	$price = 0;
else
	$price = str_replace('$','',$price);
echo "price=$price<br>";
$datePublished = $html->top(".clTime")->innerHTML();
if ($datePublished !== null)
	$datePublished = date('Y-m-d',strtotime($datePublished));
echo "published=$datePublished<br>";
echo "minSdk=$minSdk<br>";
echo "downloads=$downloads<br>";





// Amazon
echo "<b>Amazon</b><br>";
$url = "http://www.amazon.com/s/ref=nb_sb_noss?field-keywords=".$name; // amazon search
echo "<a href=\"$url\" target=\"_blank\">$url</a><br>";
$html = htmlqp($html);
if ($html->top(".newaps") !== null) {
	$url = $html->top(".newaps")->find("a")->href;
	echo "<a href=\"$url\" target=\"_blank\">$url</a><br>";
}
?>