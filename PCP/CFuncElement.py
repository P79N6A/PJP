import re

class ParamElement():
	"""docstring for paramElement"""
	paramType = ""
	paramName = ""

	
            		

class FuncElement():
	"""docstring for funcElement"""
	declaration = ""
	funcName = ""
	returnType = ""
	params = []

	def decodeParams(self, params_str):
		params_strs = params_str.split(",")
		for param_str in params_strs:
			param_name = re.split(r'\s*[\*\&]*\s*' , params_str)[-1]
			print param_name
			pass
		
		pass
		

