[#ftl]
	<footer>
		<section>
		  <aside><img alt="SiB Colombia" src="${rootURL}/images/logo_small.png"></aside>
		  <div>
		    <h5>Sistema de información sobre Biodiversidad de Colombia</h5>
		    Sede Principal Calle 28A No. 15-09 / Bogotá D.C., Colombia <br>
		  </div>
		</section>
	</footer>
	
    <script src="${rootURL}/js/jquery-1.8.1.min.js"></script>
    <script src="${rootURL}/js/bootstrap.min.js"></script>
    <script src="${rootURL}/js/jquery.fineuploader-3.6.3.min.js"></script>
    <script src="${rootURL}/js/iframe.xss.response-3.6.3.js"></script>
    
    <script type="text/javascript">  
		$(document).ready(function () {  
			$("[rel=tooltip]").tooltip();
			
			var manualuploader = new qq.FineUploader({
				element: $('#manual-fine-uploader')[0],
				request: {
					endpoint: '/procesador-cr-sib/upload.do'
				},
				autoUpload: false,
				
				dragAndDrop: {
					hideDropzones: true
				},
				validation: {
					allowedExtensions: ['xls', 'xlsx'],
					sizeLimit: 10485760, // 50 kB = 50 * 1024 bytes
					itemLimit: 2
				},
				text: {
					uploadButton: '<i class="icon-plus icon-white"></i> [@s.text name="button.select.files"/]'
				}
			});
			
			$('#triggerUpload').click(function() {
				manualuploader.uploadStoredFiles();
			});
		});
	</script>
  </body>
</html>