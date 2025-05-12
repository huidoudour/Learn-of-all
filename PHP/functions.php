<?php
function random_text( $count, $rm_similar = false ) {
	$chars = array_flip(array_merge(range(0,9),range('A','Z')));
	if ( $rm_similar) {
		unset( $chars[0], $chars[1], $chars[2], $chars['I'], $chars['O'], $chars['Z']);
	}
	for ( $i = 0, $text = ''; $i < $count; $i++) {
		$text.=array_rand($chars);
	}
	return $text;
}