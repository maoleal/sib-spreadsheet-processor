[#ftl]
[#include "/WEB-INF/pages/include/header.ftl"]
<div class="container">
	<div class="row">
	
		<div class="span8">
			<div class="content content-full-witdh">    	
				<div class="content-inner">
    				<h2>[@s.text name="sibsp.application.title.welcome"/]</h2>
					<h3>[@s.text name="sibsp.application.instructions.title"/]</h3>
		
					<ol>
						<li>[@s.text name="sibsp.application.firststep"/]</li>
						<li>[@s.text name="sibsp.application.secondstep"/]</li>
						<li>[@s.text name="sibsp.application.thirdstep"/]</li>
						<li>[@s.text name="sibsp.application.fourthstep"/]</li>
					</ol>
					<hr/>
					<h3>[@s.text name="sibsp.application.templateslist.title"/]</h3>
					<!--
					<dl class="dl-horizontal">
						<dt>[@s.text name="sibsp.application.metadatafile.name"/]</dt>
						<dd>[@s.text name="sibsp.application.metadatafile.description"/]<br/> <a class="btn btn-success btn-variacion" href="${rootURL}/templates/Plantilla_Metadatos_v2.0.xlsx"><i class="icon-download-alt"></i>[@s.text name="button.download"/] (Excel 2007-2013)</a></dd>
					</dl>
					-->
					<dl class="dl-horizontal">
						<dt>[@s.text name="sibsp.application.taxonomic.name"/]</dt>
						<!--<dd>[@s.text name="sibsp.application.taxonomic.description"/]<br/>--> <a class="btn btn-success btn-variacion" href="${rootURL}/templates/Plantilla_reporte_al_sib.xlsx"><i class="icon-download-alt"></i>[@s.text name="button.download"/] (Excel 2007-2013)</a></dd>
					</dl>
					<dl class="dl-horizontal">
						<dt>[@s.text name="sibsp.application.ocurrence.name"/]</dt>
						<!--<dd>[@s.text name="sibsp.application.ocurrence.description"/]<br/>--> <a class="btn btn-success btn-variacion" href="${rootURL}/instructive/Instructivo_Plantilla_Reporte_al_SiB.pdf"><i class="icon-download-alt"></i>[@s.text name="button.download"/] (PDF)</a></dd>
					</dl>
				</div>	
			</div>
			[@s.fielderror cssClass="alert alert-error" fieldName="file"/]
		</div>

			
		
		<div class="span4">
			<div class="content content-full-witdh">    	
				<div class="content-inner">
				
					<div id="new-resource">
						<h3>[@s.text name="sibsp.application.newtemplate.title"/]</h3>
						[#include "include/upload_new_template.ftl"/]
					</div>
    
				</div>	
			</div>
		</div>
		
	</div>
</div> <!-- /container -->
[#include "/WEB-INF/pages/include/footer.ftl"]