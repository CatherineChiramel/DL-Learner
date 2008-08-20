<?php
function subjectToURI($subject)
{
	//if the subject is already a URI return it
	if (strpos($subject,"http://dbpedia.org/resource/")===0)
		return $subject;
	//delete whitespaces at beginning and end
	$subject=trim($subject);
	//get first letters big
	$subject=ucfirst($subject);
	//replace spaces with _
	$subject=str_replace(' ','_',$subject);
	//add the uri
	$subject="http://dbpedia.org/resource/".$subject;
	
	return $subject;
}

function getTagCloud($tags,$label)
{
	if (isset($tags['NoCategory'])){
		$nc=true;
		unset($tags['NoCategory']);
	}
	else $nc=false;
	
	$max=max($tags);
	$min=min($tags);
	$diff=$max-$min;
	$distribution=$diff/3;
	
	$ret="<p>";
	$ret.='<a style="font-size:xx-large;" href="#" onclick="document.getElementById(\'hidden_class\').value=\'all\';show_results(\'all\',document.getElementById(\'hidden_number\').value);">All</a>&nbsp;';
	if ($nc) $ret.='<a style="font-size:xx-small;" href="#" onclick="document.getElementById(\'hidden_class\').value=\'NoCategory\';show_results(\'NoCategory\',document.getElementById(\'hidden_number\').value);">No Category</a>&nbsp;';
	foreach ($tags as $tag=>$count){
		if ($count==$min) $style="font-size:xx-small;";
		else if ($count==$max) $style="font-size:xx-large;";
		else if ($count>($min+2*$distribution)) $style="font-size:large;";
		else if ($count>($min+$distribution)) $style="font-size:medium;";
		else $style="font-size:small;";
		
		//$tag_with_entities=htmlentities("\"".$tag."\"");
		$ret.='<a style="'.$style.'" href="#" onclick="document.getElementById(\'hidden_class\').value=\''.$tag.'\';show_results(\''.$tag.'\',document.getElementById(\'hidden_number\').value);">'.$label[$tag].'</a>&nbsp;';
	}
	$ret.="</p><br/>";
	return $ret;
}

function getResultsTable($names,$labels,$classes,$number)
{
	$ret="<p>These are your Searchresults. Show best ";
	for ($k=10;$k<125;){
		$ret.="<a href=\"#\" onclick=\"search_it('label='+document.getElementById('label').value+'&number=".$k."');return false;\"";
		if ($k==$number) $ret.=" style=\"text-decoration:none;\"";
		else $ret.=" style=\"text-decoration:underline;\"";
		$ret.=">".($k)."</a>";
		if ($k!=100) $ret.=" | ";
		if($k==10) $k=25;
		else $k=$k+25;
	}
	$ret.="</p><br/>";
	$i=0;
	$display="block";
	$ret.="<div id=\"results\">";
	while($i*25<count($names))
	{
		for ($j=0;($j<25)&&(($i*25+$j)<count($names));$j++)
		{
			$name=$names[$i*25+$j];
			$label=$labels[$i*25+$j];
			if (strlen($label)==0) $label=urldecode(str_replace("_"," ",substr (strrchr ($name, "/"), 1)));
			$class="";
			$k=0;
			foreach ($classes[$i*25+$j] as $cl){
				if ($k!=count($classes[$i*25+$j])-1) $class.=$cl.' ';
				else $class.=$cl;
				$k++;
			}
			$ret.='<p style="display:'.$display.'">&nbsp;&nbsp;&nbsp;&nbsp;'.($i*25+$j+1).'.&nbsp;<a href="" class="'.$class.'" onclick="get_article(\'label='.$name.'&cache=-1\');return false;">'.utf8_to_html($label).'</a></p>';
		}
		$i++;
		$display="none";
	}
	$ret.='<input type="hidden" id="hidden_class" value="all"/><input type="hidden" id="hidden_number" value="0"/></div><br/><p style="width:100%;text-align:center;" id="sitenumbers">';
	for ($k=0;$k<$i;$k++){
		$ret.="<span>";
		if ($k!=0) $ret.=" | ";
		$ret.="<a href=\"#\" onclick=\"document.getElementById('hidden_number').value='".(25*$k)."';show_results(document.getElementById('hidden_class').value,".(25*$k).");\"";
		if ($k==0) $ret.=" style=\"text-decoration:none;\"";
		else $ret.=" style=\"text-decoration:underline;\"";
		$ret.=">".($k+1)."</a>";
		$ret.="</span>";
	}
	$ret.="</p>";
	return $ret;
}

function utf8_to_html($string)
{
	$string=str_replace("u00C4","&Auml;",$string);
	$string=str_replace("u00D6","&Ouml;",$string);
	$string=str_replace("u00DC","&Uuml;",$string);
	$string=str_replace("u00E4","&auml;",$string);
	$string=str_replace("u00F6","&ouml;",$string);
	$string=str_replace("u00FC","&uuml;",$string);
	$string=str_replace("u0161","&scaron;",$string);
	
	return $string;
}

function getCategoryResultsTable($names,$labels,$category,$number)
{
	$ret="<p>These are your Searchresults. Show best ";
	for ($k=10;$k<125;){
		$ret.="<a href=\"#\" onclick=\"getSubjectsFromCategory('category=".$category."&number=".$k."');return false;\"";
		if ($k==$number) $ret.=" style=\"text-decoration:none;\"";
		else $ret.=" style=\"text-decoration:underline;\"";
		$ret.=">".($k)."</a>";
		if ($k!=100) $ret.=" | ";
		if($k==10) $k=25;
		else $k=$k+25;
	}
	$ret.="</p><br/>";
	$i=0;
	$display="block";
	$ret.="<div id=\"results\">";
	while($i*25<count($names))
	{
		for ($j=0;($j<25)&&(($i*25+$j)<count($names));$j++)
		{
			$name=$names[$i*25+$j];
			$label=$labels[$i*25+$j];
			if (strlen($label)==0) $label=urldecode(str_replace("_"," ",substr (strrchr ($name, "/"), 1)));
			$ret.='<p style="display:'.$display.'">&nbsp;&nbsp;&nbsp;&nbsp;'.($i*25+$j+1).'.&nbsp;<a class="all" href="" onclick="get_article(\'label='.$name.'&cache=-1\');return false;">'.utf8_to_html($label).'</a></p>';
		}
		$i++;
		$display="none";
	}
	$ret.='<input type="hidden" id="hidden_class" value="all"/><input type="hidden" id="hidden_number" value="0"/></div><br/><p style="width:100%;text-align:center;" id="sitenumbers">';
	for ($k=0;$k<$i;$k++){
		$ret.="<span>";
		if ($k!=0) $ret.=" | ";
		$ret.="<a href=\"#\" onclick=\"document.getElementById('hidden_number').value='".(25*$k)."';show_results(document.getElementById('hidden_class').value,".(25*$k).");\"";
		if ($k==0) $ret.=" style=\"text-decoration:none;\"";
		else $ret.=" style=\"text-decoration:underline;\"";
		$ret.=">".($k+1)."</a>";
		$ret.="</span>";
	}
	$ret.="</p>";
	return $ret;
}

function getBestSearches($names,$labels)
{
	$ret="<div id=\"best-results\">";
	for ($j=0;($j<10)&&$j<count($names);$j++)
	{
		$name=$names[$j];
		$label=$labels[$j];
		if (strlen($label)==0) $label=urldecode(str_replace("_"," ",substr (strrchr ($name, "/"), 1)));
		$ret.='&nbsp;'.($j+1).'.&nbsp;<a href="" onclick="get_article(\'label='.$name.'&cache=-1\');return false;">'.utf8_to_html($label).'</a><br/>';
	}
	$ret.="</div>";
	return $ret;
}

function getPrintableURL($url)
{
	$parts=explode('/',$url);
	return $parts[0].'//'.$parts[2].'/w/index.php?title='.$parts[4].'&printable=yes';
}

function setRunning($id,$running)
{
	if(!is_dir("temp")) mkdir("temp");
	$file=fopen("./temp/".$id.".temp","w");
	fwrite($file, $running);
	fclose($file);
}

function get_triple_table($triples) {

	$table = '<table border="0"><tr><td><b>Predicate</b></td><td><b>Object</b></td></tr>';
	$i=1;
	foreach($triples as $predicate=>$object) {
		if ($i>0) $backgroundcolor="eee";
		else $backgroundcolor="ffffff";
		$table .= '<tr style="background-color:#'.$backgroundcolor.';"><td><a href="'.$predicate.'" target="_blank">'.nicePredicate($predicate).'</a></td>';
		$table .= '<td><ul>';
		foreach($object as $element) {
			if ($element['type']=="uri"){
				if (strpos($element['value'],"http://dbpedia.org/resource/")===0&&substr_count($element['value'],"/")==4) $table .= '<li><a href="#" onclick="get_article(\'label='.$element['value'].'&cache=-1\');return false;">'.urldecode($element['value']).'</a></li>';
				else $table .= '<li><a href="'.$element['value'].'" target="_blank">'.urldecode($element['value']).'</a></li>';
			}
			else $table .= '<li>'.$element['value'].'</li>';
		}
		$table .= '</ul></td>';
		$i*=-1;
	}
	$table .= '</table>';
	return $table;
}

function nicePredicate($predicate)
{
	if (strripos ($predicate, "#")>strripos ($predicate, "/")){
		$namespace=substr ($predicate,0,strripos ($predicate, "#"));
		$name=substr ($predicate,strripos ($predicate, "#")+1);
	}
	else{
		$namespace=substr ($predicate,0,strripos ($predicate, "/"));
		$name=substr ($predicate,strripos ($predicate, "/")+1);
	}
	
	switch ($namespace){
		case "http://www.w3.org/2000/01/rdf-schema": 	$namespace="rdfs";
													 	break;
		case "http://www.w3.org/2002/07/owl": 		 	$namespace="owl";
													 	break;
		case "http://xmlns.com/foaf/0.1":			 	$namespace="foaf";
													 	break;
		case "http://dbpedia.org/property":			 	$namespace="p";
													 	break;
		case "http://www.w3.org/2003/01/geo/wgs84_pos":	$namespace="geo";
													 	break;
		case "http://www.w3.org/2004/02/skos/core":		$namespace="skos";
													 	break;
		case "http://www.georss.org/georss/point":		$namespace="georss";
													 	break;	
	}
	
	//fl�che has strange url
	$name=str_replace('fl_percent_C3_percent_A4che','fl%C3%A4che',$name);
	return $namespace.':'.urldecode($name);
}

function formatClassArray($ar) {
	mysql_connect('localhost','navigator','dbpedia');
	mysql_select_db("navigator_db");
	$string="<ul>";
	for($i=0; $i<count($ar); $i++) {
		$query="SELECT label FROM categories WHERE category='".$ar[$i]['value']."' LIMIT 1";
		$res=mysql_query($query);
		$result=mysql_fetch_array($res);
		if ($ar[$i]['value']!="http://xmlns.com/foaf/0.1/Person") $string .= '<li>' . formatClass($ar[$i]['value'],$result['label']).'</li>';
	}
	return $string."</ul>";
}

// format a class nicely, i.e. link to it and possibly display
// it in a better way
function formatClass($className,$label) {
	$yagoPrefix = 'http://dbpedia.org/class/yago/';
	if(substr($className,0,30)==$yagoPrefix) {
		return $label.'&nbsp;&nbsp;&nbsp;<a href="#" onclick="getSubjectsFromCategory(\'category='.$className.'&number=10\');">&rarr; search Instances</a>&nbsp;&nbsp;<a href="#" onclick="get_class(\'class='.$className.'&cache=-1\');">&rarr; show Class in Hierarchy</a>';	
	// DBpedia is Linked Data, so it makes always sense to link it
	// ToDo: instead of linking to other pages, the resource should better
	// be openened within DBpedia Navigator
	} else if(substr($className,0,14)=='http://dbpedia') {
		return '<a href="'.$className.'" target="_blank">'.$className.'</a>';
	} else {
		return $className;
	}
}

function arrayToCommaSseparatedList($ar) {
	$string = $ar[0];
	for($i=1; $i<count($ar); $i++) {
		$string .= ', ' . $ar[$i];
	}
	return $string;
}

function show_Interests($sess)
{
	$ret=array();
	$ret[0]="";
	$ret[1]="";
	if (isset($sess['positive'])) foreach($sess['positive'] as $name=>$lab){
		$ret[0].=$lab." <a href=\"\" onclick=\"toNegative('subject=".$name."&label=".$lab."');return false;\"><img src=\"images/minus.jpg\" alt=\"Minus\"/></a> <a href=\"\" onclick=\"removePosInterest('subject=".$name."');return false;\"><img src=\"images/remove.png\" alt=\"Delete\"/></a><br/>";
	}
	if (isset($sess['negative'])) foreach($sess['negative'] as $name=>$lab){
		$ret[1].=$lab." <a href=\"\" onclick=\"toPositive('subject=".$name."&label=".$lab."');return false;\"><img src=\"images/plus.jpg\" alt=\"Plus\"/></a> <a href=\"\" onclick=\"removeNegInterest('subject=".$name."');return false;\"><img src=\"images/remove.png\" alt=\"Delete\"/></a><br/>";
	}
	
	return $ret;
}

function getClassView($fathers,$childs,$title,$class)
{
	$ret='This is the class view. You can browse through the hierarchy and search for instances of classes.<br/><br/>';
	$childButtons=true;
	if (strlen($childs)==0){
		$childs='There are no Child classes';
		$childButtons=false;
	}
	$fatherButtons=true;
	if (strlen($fathers)==0){
		$fathers='There are no Father classes';
		$fatherButtons=false;
	}
			
	$ret.='<table border="0" style="text-align:left;width:100%">';
	$ret.='<tr><td style="width:90%"><b>Father classes</b></td></tr>';
	$ret.='<tr style="height:10px"><td></td></tr>';
	$ret.='<tr><td>'.$fathers.'</td></tr>';
	$ret.='<tr style="height:10px"><td></td></tr>';
	$ret.='<tr><td>';
	if ($fatherButtons) $ret.='<input style="width:70px" type="button" value="Instances" class="button" onclick="getSubjectsFromCategory(\'category=\'+document.getElementById(\'fatherSelect\').options[document.getElementById(\'fatherSelect\').selectedIndex].value+\'&number=10\');" title="Search Instances of Father class."/>&nbsp;&nbsp;<input style="width:70px" type="button" value="Class" class="button" onclick="get_class(\'class=\'+document.getElementById(\'fatherSelect\').options[document.getElementById(\'fatherSelect\').selectedIndex].value+\'&cache=-1\');" title="Show Father class in class view."/>';
	$ret.='</td></tr>';
	$ret.='<tr style="height:20px"><td><hr/></td></tr>';
	$ret.='<tr><td><b>Current class</b></td></tr>';
	$ret.='<tr style="height:10px"><td></td></tr>';
	$ret.='<tr><td><b>'.$title.'</b></td></tr>';
	$ret.='<tr style="height:10px"><td></td></tr>';
	$ret.='<tr><td>';
	$ret.='<input style="width:70px" type="button" value="Instances" class="button" onclick="getSubjectsFromCategory(\'category='.$class.'&number=10\');" title="Search Instances of Shown class."/>';
	$ret.='</td></tr>';
	$ret.='<tr style="height:20px"><td><hr/></td></tr>';
	$ret.='<tr><td style="width:30%"><b>Child classes</b></td></tr>';
	$ret.='<tr style="height:10px"><td></td></tr>';
	$ret.='<tr><td>'.$childs.'</td></tr>';
	$ret.='<tr style="height:10px"><td></td></tr>';
	$ret.='<tr><td>';
	if ($childButtons) $ret.='<input style="width:70px" type="button" value="Instances" class="button" onclick="getSubjectsFromCategory(\'category=\'+document.getElementById(\'childSelect\').options[document.getElementById(\'childSelect\').selectedIndex].value+\'&number=10\');" title="Search Instances of Child class."/>&nbsp;&nbsp;<input style="width:70px" type="button" value="Class" class="button" onclick="get_class(\'class=\'+document.getElementById(\'childSelect\').options[document.getElementById(\'childSelect\').selectedIndex].value+\'&cache=-1\');" title="Show Child class in class view."/>';
	$ret.='</td></tr>';
	$ret.='</table>';
				
	return $ret;
}
?>