package no.stelar7.cdragon.util.types;

import no.stelar7.cdragon.util.handlers.UtilHandler;

import javax.script.*;

public class JSPrettier
{
    
    private static final String BEAUTIFY_JS_RESOURCE = "scripts/pretty.js";
    private static final String BEAUTIFY_METHOD_NAME = "js_beautify";
    
    private final ScriptEngine engine;
    
    public JSPrettier() throws ScriptException
    {
        engine = new ScriptEngineManager().getEngineByName("nashorn");
        
        // this is needed to make self invoking function modules work
        // otherwise you won't be able to invoke your function
        engine.eval("var global = this;");
        engine.eval(UtilHandler.readInternalAsString(BEAUTIFY_JS_RESOURCE));
    }
    
    public String beautify(String javascriptCode) throws ScriptException, NoSuchMethodException
    {
        return (String) ((Invocable) engine).invokeFunction(BEAUTIFY_METHOD_NAME, javascriptCode);
    }
}
