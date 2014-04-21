<?php 
	function getApiLevel($str) {
		$apiLevel = array(
				"4.2"	=> 17,
				"4.1"	=> 16,
				"4.0.3"	=> 15,
				"4.0"	=> 14,
				"3.2"	=> 13,
				"3.1"	=> 12,
				"3.0"	=> 11,
				"2.3.3"	=> 10,
				"2.3"	=> 9,
				"2.2"	=> 8,
				"2.1"	=> 7,
				"2.0.1"	=> 6,
				"2.0"	=> 5,
				"1.6"	=> 4,
				"1.5"	=> 3,
				"1.1"	=> 2,
				"1.0"	=> 1
				);
		return($apiLevel[$str]);
	}
?>