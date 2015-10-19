<%@ page import="org.openmrs.api.context.Context" %>
<%@ page import="org.openmrs.User" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="org.openmrs.module.retrosecurity.api.RetroSecurityService" %>


<%@ include file="/WEB-INF/template/include.jsp"%>
<%@ include file="/WEB-INF/template/header.jsp"%>

<%@ include file="template/localHeader.jsp"%>

<p>Hello ${user.systemId}!</p>

<%! String value; %>
<%! RetroSecurityService retroSecurityService = Context.getService(RetroSecurityService.class); %>
<%! String currentUser = Context.getAuthenticatedUser().toString(); %>
<%! ArrayList glassBrokenUsers = new ArrayList(); %>

<% 
//if(request.getParameter("buttonName") != null) {
retroSecurityService = Context.getService(RetroSecurityService.class);
if(request.getParameterNames() != null) {
	value = request.getParameter("buttonName");
}
if (value != null) {
	if (value.equals("BREAK")) {// call break the glass method
		if (!glassBrokenUsers.contains(Context.getAuthenticatedUser())){
			glassBrokenUsers.add(Context.getAuthenticatedUser());
		}
		out.println("The glass is broken!");
		out.println("Current user is " + Context.getAuthenticatedUser() + ".");
		retroSecurityService.breakTheGlass();
	}
	else if (value.equals("BUILD")) {
	}
	else if (value.equals("QUERY")) {
		out.println(retroSecurityService.queryLog());
	}
	else if (value.equals("LOG")) {
		out.println(retroSecurityService.readDerivedLog());
	}
}
%>
   

        <FORM NAME="form1">
            <INPUT TYPE="HIDDEN" NAME="buttonName">
            <INPUT TYPE="BUTTON" VALUE="BREAK THE GLASS" ONCLICK="button1()" 
		<% if (glassBrokenUsers.contains(Context.getAuthenticatedUser())) { %> disabled <% } %> >
            <INPUT TYPE="BUTTON" VALUE="BUILD THE GLASS" ONCLICK="button2()" disabled >
			<!-- <% if (!currentUser.equals("admin")) { %> disabled <% } %> -->
            <INPUT TYPE="BUTTON" VALUE="INSTANT RESULTS" ONCLICK="button3()">
	    <INPUT TYPE="BUTTON" VALUE="THE LOG" ONCLICK="button4()">
        </FORM>

        <SCRIPT LANGUAGE="JavaScript">
            <!--
            function button1()
            {
                document.form1.buttonName.value = "BREAK"
                form1.submit()
            }    
            function button2()
            {
                document.form1.buttonName.value = "BUILD"
                form1.submit()
            }
            function button3()
            {
                document.form1.buttonName.value = "QUERY"
                form1.submit()
            }
	    function button4()
            {
                document.form1.buttonName.value = "LOG"
                form1.submit()
            }
    
            // --> 
        </SCRIPT>


<%@ include file="/WEB-INF/template/footer.jsp"%>
