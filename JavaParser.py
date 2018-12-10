import javalang
import sys

reload(sys)
sys.setdefaultencoding( "utf-8" )

fd = open("JavaFile/WGPlatform.java" , "r+")
code_str = fd.read()

tree = javalang.parse.parse(code_str)

for path, node in tree.filter(javalang.tree.MethodDeclaration):
	print(node.name)
	print("\n\n")
	print("-----------------------------------")
	print("\n\n")
	pass
