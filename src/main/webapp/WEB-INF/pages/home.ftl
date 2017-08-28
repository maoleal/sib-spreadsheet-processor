[#ftl]
[#include "/WEB-INF/pages/include/header.ftl"]
<div class="container">

	<div class="row">
		<h1>[@s.text name="sibsp.application.title.welcome"/]</h1>
		<div class="content-inner span5 intro round">
		<img src="${rootURL}/images/conversion_ico.png" style="display:inline-block; vertical-align:top"/>	
		<p>El procesador es una herramienta que transforma la plantilla de Excel preconfigurada por el SiB Colombia, en un archivo Darwin Core (formato .zip). El archivo se utilizará en el siguiente paso.<br/><br/>El procesador constituye el  tercer paso para obtener el Certificado de Reporte CR-SiB en el marco de los Decretos 1376 y 3016 del 2013. El archivo generado se utilizará en el siguiente paso del proceso.</p>
		</div>
		<div class="span4" style="margin-left:40px !important;">  
				<div class="content-inner blue round">
					[@s.text name="sibsp.application.fourthstep"/]
					<hr/>
					<div id="new-resource">
					[#include "include/upload_new_template.ftl"/]
					</div>
			</div>
		</div>
	</div>
		[#if fieldErrors?exists]
			[#assign num=fieldErrors.values()?size]
			[#if num != 0]
				<div class="alert alert-error">
					<p>[@s.text name="sibsp.application.error"/]</p>
					[#list fieldErrors.values() as item]
						<li><span>${item[0]}</span></li>
					[/#list]
				</div>
			[/#if]
		[/#if]
	
		
	
	
</div> <!-- /container -->
[#include "/WEB-INF/pages/include/footer.ftl"]