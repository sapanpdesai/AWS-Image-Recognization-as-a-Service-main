<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<html xmlns="http://www.w3.org/1999/xhtml"
	xmlns:th="https://www.thymeleaf.org"
	xmlns:sec="https://www.thymeleaf.org/thymeleaf-extras-springsecurity3">
<head>
<!-- Basic Page Needs
    ================================================== -->
<meta charset="utf-8">
<!--[if IE]><meta http-equiv="x-ua-compatible" content="IE=9" /><![endif]-->
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Image Recognizer</title>
<meta name="description" content="Your Description Here">
<meta name="keywords"
	content="bootstrap themes, portfolio, responsive theme">
<meta name="author" content="ThemeForces.Com">


<!-- Bootstrap -->
<link rel="stylesheet" type="text/css" href="css/bootstrap.css">


<!-- Stylesheet
    ================================================== -->
<link rel="stylesheet" type="text/css" href="css/style.css">
<link rel="stylesheet" type="text/css" href="css/responsive.css">

</head>
<body>
	<%=request.getAttribute("classificationResult")%>

	<div id="tf-home">
		<div class="overlay">

			<nav id="tf-menu" class="navbar navbar-default">

				<!-- Brand and toggle get grouped for better mobile display -->
				<div class="navbar-header">

					<h1>

						<a class="navbar-brand logo">Image Recognizer</a>
					</h1>
				</div>
			</nav>
		</div>
	</div>


	<div id="tf-contact">
		<div class="container">
			<div class="section-title">
				<h3>Upload Images</h3>
				<p></p>
				<hr>
			</div>

			<div class="space"></div>

			<div class="row">
				<div class="col-md-6 col-md-offset-3">
					<form id="contact" class="dynamic" action="/imagerecognization"
						enctype="multipart/form-data" method="post">

						<h4 align="left">
							Upload Images <br> <br> <input id="imageurl"
								multiple="multiple" name="imageurl" type="file"
								accept="image/png, image/jpeg, image/jpg" /> <br>



							<div id="dynamic"></div>
							<div>
								<button type="submit" class="btn btn-primary my-btn dark">Submit</button>
								<button type="reset" class="btn btn-primary my-btn dark">Reset</button>
							</div>
					</form>
				</div>
			</div>
		</div>
	</div>
	<div class="container">
		<table class="table table-bordered">
			<thead>
				<tr>
					<th>Image Name</th>
					<th>Classification Result</th>
				</tr>
			</thead>
			<tbody>
				<tr th:each="instance : ${classificationResult}">
					<td th:text="${instance.key}">keyvalue</td>
					<td th:text="${instance.value}">num</td>
				</tr>
			</tbody>
		</table>
	</div>
</body>
</html>