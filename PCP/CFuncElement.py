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
			if ("=" in param_str):

				param_str = re.split(r'\s*=\s*' , param_str)[0]
				pass
			
			param_strs = re.split(r'\s*[\*\&]*\s*' , param_str)
			param_name = param_strs[-1].strip()
			param_type = param_str[:-len(param_name)].strip()
			if(len(param_name) > 0):
				print param_name + ' : ' +param_type
				pass
				
			
			pass
		
		pass
		

