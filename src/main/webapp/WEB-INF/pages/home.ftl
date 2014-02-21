[#ftl]
[#include "/WEB-INF/pages/include/header.ftl"]
<div class="container">

	<div class="row">
		<h1 class="intro">[@s.text name="sibsp.application.title.welcome"/]</h1>
		<p class="intro">El validador de Darwin Core archive para archivos de excel es una herramienta web que transforma archivos de MS Excel preconfigurados por el SiB Colombia en archivos de Darwin Core de GBIF. Desntro de un archivo Darwin Core existen dos archivos: una hoja de cálculo contiene el perfil de metadatos y un segundo archivo contiene los datos.</p>

		<a class="span3" href="${rootURL}/instructive/Instructivo_Plantilla_Reporte_al_SiB.pdf">
				<div class="content-inner">
					<span class="numbers">1</span>
						<p>[@s.text name="sibsp.application.firststep"/]</p>
					<hr/>
					<img src="${rootURL}/images/pdf_ico.png"/>
					<strong>DESCARGAR<br/>INSTRUCTIVO</strong>
			</div>
		</a>
			[#if fieldErrors?exists]
				[#assign num=fieldErrors.values()?size]
				[#if num != 0]
					<div class="alert alert-error">	
						<p>[@s.text name="sibsp.application.error.</p>missing"/]
						[#list fieldErrors.values() as item]
						<li><span>${item[0]}</span></li>
						[/#list]
						
					</div>
				[/#if]
			[/#if]

		
		<a class="span3" href="${rootURL}/templates/Plantilla_de_Registros_Biologicos_CPSiB.xlsx">
				<div class="content-inner">
					<span class="numbers">2</span>
					<p>[@s.text name="sibsp.application.secondstep"/]</p>
					<hr/>
					<img src="${rootURL}/images/xls_ico.png"/>
					<strong>DESCARGAR<br/>PLANTILLA</strong><br/>
					Excel 2007-2013
				</div>
		</a>
		<div class="span3">	
				<div class="content-inner">
					<span class="numbers">3</span>
					<p>[@s.text name="sibsp.application.thirdstep"/]</p>
					<hr/>
					<img src="${rootURL}/images/fields_ico.png"/>
			</div>
		</div>
		<div class="span3">  	
				<div class="content-inner">
					<span class="numbers">4</span>
					<p>[@s.text name="sibsp.application.fourthstep"/]</p>
					<hr/>
					<div id="new-resource">
					[#include "include/upload_new_template.ftl"/]
					</div>
			</div>
		</div>
		<a class="span3" href="http://www.sibcolombia.net/web/sib/herramienta-de-publicacion-de-conjuntos-de-datos" target="_blank"> 	
				<div class="content-inner">
					<span class="numbers">5</span>
					<p>[@s.text name="sibsp.application.fifthstep"/]</p>
					<hr/>
					<img src="${rootURL}/images/ipt_ico.png"/>
			</div>
		</a>
	</div>
</div> <!-- /container -->
[#include "/WEB-INF/pages/include/footer.ftl"]