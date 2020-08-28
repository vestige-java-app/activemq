
JspRuntimeContext is getting system policy -> prevent it
Tld jar are scanned from classpath -> load tld jar with maven resolver and create classpath property, in WebInfConfiguration always read classpath property
TypeUtil is creating memory leak because of LambdaForm$DMH -> do not use MethodType.methodType in TypeUtil

JDK 11 GC is not working ... JDK 8 is OK
