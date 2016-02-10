function header()
{
   var part = document.title.split("/");
   var maintitle = part[0];
   var subtitle = '';
   var homepage = 'index.html';
   var prefix = '';
   for(i = 1; i < part.length; i++) prefix += '../';
   
   // Define header area
   var html = [
'   <div class="row header with-border">',
'      <div class="span9 logo">',
'         <h1><a href="' + prefix + homepage + '">' + maintitle + '</a>'
   ]
   for(i = 1; i < (part.length-1); i++) {
   	prefix = '';
   	for(j = i+1; j < part.length; j++) prefix += '../';
   	html.push(
   	   ' &bull; <a href="' + prefix + 'index.html">' + part[i] + '</a></span>'
   	);
   }
   if(part.length > 1) {
      html.push(
         ' &bull; <span class="subtitle">' + part[part.length-1] + '</span></span>'
      );
   }
   html.push(
'      </h1>',
'      </div>',
'      <div class="span3">',
'         <div class="navigation">'
   );
   
if(part.length > 1) { // Add nav area
   html.push(
'            <ul>',
'               <li><a href="' + homepage + '">home</a></li>',
'            </ul>'
   );
}

// Close nav area
   html.push(
'         </div><!-- navigation -->',
'      </div><!-- span -->'
   );


html.push(
'   </div><!-- header -->',
'   <div class="row">'
);
   for(x in html) { document.write(html[x] + "\n"); }
}

function footer() {
   var html = [
'</div><!-- row -->',
'   <div class="row footer">',
'      <div class="span12">',
'        Copyright <a href="license.html">&copy;</a> 2013. Regents University of California.',
'      </div>',
'   </div>'
];
   for(x in html) { document.write(html[x] + "\n"); }
}