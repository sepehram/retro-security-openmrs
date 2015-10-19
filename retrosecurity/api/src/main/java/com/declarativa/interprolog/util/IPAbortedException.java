/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog.util;
/** An Exception thrown when Prolog is aborted from the Java side, typically due to InterProlog's internal handling of a shutdown request
*/
@SuppressWarnings("serial")
public class IPAbortedException extends IPException{
	public IPAbortedException(String s){super(s);}
}

