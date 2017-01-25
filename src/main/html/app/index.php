<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<title>Register your Facebook account with the AFEL platform</title>
<!-- Bootstrap Core CSS -->
<link
	href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css"
	rel="stylesheet">
<link rel="stylesheet" type="text/css" href="../commons/css/style.css">
<script src="https://code.jquery.com/jquery-3.1.1.min.js"
	type="text/javascript">
    </script>
</head>

<body>
	<script type="text/javascript">
        //<![CDATA[
		var apiver = 'v2.8';
		window.fbAsyncInit = function() {
			FB.init({
				appId: '648343058580264',
				xfbml: true,
				version: apiver
			});
			checkLoginState()
		};
		(function(d, s, id) {
			var js, fjs = d.getElementsByTagName(s)[0];
			if (d.getElementById(id)) {
				return;
			}
			js = d.createElement(s);
			js.id = id;
			js.src = "//connect.facebook.net/en_US/sdk.js";
			fjs.parentNode.insertBefore(js, fjs);
		}(document, 'script', 'facebook-jssdk'));
        //]]>
      </script>

	<div id="main">
		<div class="content">
		
<?php
// ini_set ( 'display_errors', 1 ); // show errors
// error_reporting ( - 1 ); // of all levels

$ecapi_conf = <<<EOC
{}
EOC;
function update_registration($afel_username, $fb_token, $afel_dataset, $afel_key, $ecapi) {
	// clear old registrations for this user
	// (OAuth tokens have most likely expired)
	$path = 'registered';
	$row = "$afel_username $fb_token $afel_dataset $afel_key $ecapi\n";
	$remove = $afel_username;
	$lines = file ( $path, FILE_IGNORE_NEW_LINES | LOCK_EX );
	foreach ( $lines as $key => $line ) {
		if (0 === strpos ( $line, $remove ))
			unset ( $lines [$key] );
	}
	// and add the new registration
	$lines [] = $row;
	$data = implode ( "\n", array_values ( $lines ) );
	$file = fopen ( $path, 'w' );
	fwrite ( $file, $data );
	fclose ( $file );
	// file_put_contents($path, $row, FILE_APPEND | LOCK_EX);
}

$done = false;
if ($_SERVER ['REQUEST_METHOD'] === 'POST') {
	if (empty ( $_POST ["username"] ) || empty ( $_POST ["password"] )) {
		$error = "<p>Please enter your (non-empty) username and password for the AFEL platform.</p>";
	}
	if (empty ( $_POST ["fb-token"] )) {
		$error .= "<p>Please ensure you have signed into Facebook and authorised this app.</p>";
	} else {
		$dstype = "AFEL User Facebook Activity";
		$url = "http://data.afel-project.eu/catalogue/index.php/newuserdataset";
		$data = "username=" . $_POST ['username'] . "&password=" . $_POST ['password'] . "&type=" . $dstype . "&description=Facebook activity data from user " . $_POST ['username'] . "." . "&ecapiconf=" . $ecapi_conf . "&reuse_if_match=true";
		$ch = curl_init ();
		curl_setopt ( $ch, CURLOPT_URL, $url );
		curl_setopt ( $ch, CURLOPT_POST, 5 );
		curl_setopt ( $ch, CURLOPT_POSTFIELDS, $data );
		curl_setopt ( $ch, CURLOPT_RETURNTRANSFER, true );
		$result = curl_exec ( $ch );
		$curlinfo = curl_getinfo ( $ch );
		$http_status = $curlinfo ['http_code'];
		curl_close ( $ch );
		$res = json_decode ( $result );
		if (! isset ( $res->key ) || ! isset ( $res->dataset )) {
			$error = "Issue setting up dataset with AFEL Data Platform";
		} elseif (409 == $http_status) { 
			// Manage HTTP Conflict to reuse dataset
			update_registration ( $_POST ['username'], $_POST ['fb-token'], $res->dataset, $res->key, $res->ecapi );
			$done = true;
		} elseif (isset ( $res->error )) {
			$error = "Error connecting to AFEL Platform - " . $res->error;
		} else {
			update_registration ( $_POST ['username'], $_POST ['fb-token'], $res->dataset, $res->key, $res->ecapi );
			$done = true;
		}
	}
}
?>

	<h1>Register your Facebook account into the AFEL platform</h1>

	<div class="message">
<?php if (! $done) : ?>
      In order to register your Facebook activity in the AFEL Data Platform, 
      please provide your login and password on the AFEL platform and sign 
      into your Facebook account. By registering your Facebook account, 
      you accept the <a
					href="http://data.afel-project.eu/catalogue/index.php/terms-facebook">
					Terms and Conditions</a> of this tool and of the AFEL platform.
<?php else : ?>
      Your Facebook account has been registered - thank you!
      <p><a href="/catalogue/user-dashboard/"><<< Go to AFEL Dashboard</a></p>
<?php endif; ?>
<?php if (!$done) : ?>
	</div>
	<div class="message">
		If you do not yet have an account on the AFEL platform, please <a
			href="http://data.afel-project.eu/catalogue/wp-login.php?action=register"
			target="_blank">register</a> first, then return to this page.
	</div>

	<form id="form" action="" method="POST">
		<div class="formfield">
			<input type="text" name="username" id="username"
				placeholder="username" />
		</div>
		<div class="formfield">
			<input type="password" name="password" id="password"
				placeholder="password" />
		</div>
		<div class="formfield" id="fb_signer">
			<input name="fb-token" type="hidden" />
			<div id="fb-signedas" style="display: none"></div>
			<a id="facebook-signin-link" href="#" class="signin-button"> <span
				class="facebook-logo"></span> <span class="signin-facebook-text">Sign
					in on Facebook</span>
			</a> <input name="if_exists_ok" type="hidden" value="true" />
		</div>
		<input type="submit" id="gobutton" value="Register Facebook Account"
			disabled />
	</form>

<?php     if (isset($error)) : ?>
	  <div id="error" style="visibility: visible;">
<?php         echo $error; ?>
	  </div>
<?php     else : ?>
	  <div id="error">There should not be any error yet...</div>
<?php     endif; ?>
<?php endif; ?>

	  <script>
var accessToken;
var fbp = "https://graph.facebook.com/";

function checkLoginState(skipFB) {
	FB.getLoginStatus(function(response) {
		var fbc = response.status === 'connected';
		$('#facebook-signin-link').toggle(!fbc);
		if (fbc && !skipFB) {
			$('#fb-signedas').empty();
			accessToken = response.authResponse.accessToken;
			getUserData();
			$('[name="fb-token"]').val(accessToken);
		}
		if(accessToken) {
		$.get(fbp + apiver + "/debug_token",{
			input_token: accessToken,
			access_token: accessToken
		}, function(data, status, req) {
			var noww = Date.now();
			if( noww > data.data.expires_at*1000 ) console.log("WARN OAuth token expired by "+data.data.expires_at*1000-noww);
		})
	}
		
		$("#fb-signedas").toggle(fbc);
		$('#gobutton').prop('disabled', !fbc || !$('[name="username"]').val())
	});
}

function getUserData() {
	$.get(fbp + apiver + "/me", {
		fields: 'id,name,picture, link',
		access_token: accessToken
	}, function(data, status, req) {
		if (data.id && data.name) { $("#fb-signedas").append(
			'<a href="'+data.link+'" target="_blank"><img src="'+data.picture.data.url+'"/></a>'
			+'<span>signed in as '
			+'<strong><a href="'+data.link+'" target="_blank">'+data.name+'</strong></a></span>'
			+'<a class="fb-signout" href="#">Sign out</a>'
			+''
		)}
	})
}
$('#form input').on('change', function(e) { if(typeof FB !== 'undefined') checkLoginState($('[name="fb-token"]').val()) });
$('#facebook-signin-link').on('click', function(e) {
	e.preventDefault();
	FB.login(function(response) {
		checkLoginState()
	}, {
		scope: 'user_about_me,user_education_history,user_actions.books,user_actions.video,user_status, public_profile,user_likes,user_posts'
	});
});
$("#fb-signedas").on('click','.fb-signout',function(e){
	e.preventDefault();
	FB.logout(function(response) {
		console.log('FB user has deauthorized.');
		checkLoginState()
	});	
});

	  </script>
			<script
				src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js"
				integrity="sha384-Tc5IQib027qvyjSMfHjOMaLkfuWVxZxUPnCJA7l2mCWNIpG9mGCD8wGNIcPD7Txa"
				crossorigin="anonymous"></script>
		</div>
	</div>

</body>
</html>