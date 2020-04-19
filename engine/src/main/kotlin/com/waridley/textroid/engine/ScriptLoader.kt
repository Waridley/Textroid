package com.waridley.textroid.engine

import java.io.File
import java.io.FileReader
import java.io.Reader
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager

@Suppress("UNCHECKED_CAST")
class ScriptLoader<T> {
	val classLoader: ClassLoader = Thread.currentThread().contextClassLoader
	val engine: ScriptEngine = ScriptEngineManager(classLoader).getEngineByExtension("kts")
	
	fun load(scriptReader: Reader): T? {
		return engine.eval(scriptReader) as T?
	}
	
	fun load(scriptPath: String): T? {
		return engine.eval(FileReader(File(scriptPath))) as T?
	}
}