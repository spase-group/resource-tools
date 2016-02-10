function sidebar() {
var html = [
'<div class="span3 sidebar">',
'   <div class="well" style="padding: 8px 0px; position: fixed;"">',
'      <ul class="nav nav-list">',
'         <li class="nav-header">Contents</li>',
'         <li><a href="#About">About ${options.base}</a></li>',
'         <li><a href="#Requirements">System Requirements</a></li>',
'         <li><a href="#Unpacking">Unpacking the Package</a></li>',
'         <li><a href="#Using">Using the Tool</a></li>',
'      </ul>',
'      <ul class="nav nav-list">',
'         <li class="nav-header">Quick Links</li>',
'         <li><a href="guide/${options.package}-${options.version}.pdf">User&#39;s Guide</a></li>',
'         <li><a href="api/index.html">Class API</a></li>',
'         <li><a href="example/index.html">Examples</a></li>',
'         <li><a href="http://release.igpp.ucla.edu/spase/resource-tools/${options.package}-${options.version}.zip">Download</a></li>',
'         <li><a href="https://github.com/spase-group/resource-tools">Source Code</a></li>',
'      </ul>',
'   </div><!-- well -->',
'   &nbsp; <!-- Content to keep span filled -->',
'</div><!-- sidebar -->'
];
   for(x in html) { document.write(html[x] + "\n"); }
}