import firebase_admin
from firebase_admin import db
from firebase_admin import credentials

import pandas as pd

import hashlib
import json
import shutil
import os

class ButtonMapGenerator():
	def __init__(self):
		file_name =  "Input/input.xlsx"
		xls = pd.ExcelFile(file_name)
		spreadsheets = xls.sheet_names.copy()
		spreadsheets.remove("COMBINE_PADS")
		spreadsheets.remove("Codes_Info")
		xls.close()

		keyCodes = {'AP':'aptavani', 'NP':'natukaka', 'AK':'akram-vignan', 'AV':'aartividhi' ,'DV':'dadavani', 'CD':'pads', 'G':'gujarati','E':'english','H':'hindi', 'O':'other', 'KP' : 'kavi-raj-pads-translated', 'DV' : 'dada-vani', 'PD' : 'pads'}
		filesByKey = {}

		if not os.path.isdir('Output/Button-Maps/Button-Maps'):
			os.makedirs('Output/Button-Maps/Button-Maps/')

		for spreadsheetName in spreadsheets:
			df = pd.read_excel(file_name, sheet_name=spreadsheetName)

			for line in df.values:
				#Only use data if line is not empty
				if len(line) >= 10 and not (pd.isnull(line[0]) or pd.isnull(line[1]) or pd.isnull(line[6]) or pd.isnull(line[9])):
					bookType = line[1] if line[1] != 'BT' else line[0]
					folderPath = keyCodes[bookType] + '/'
					folderPath = 'books/' + folderPath if line[1] == 'BT' else folderPath
					if not bookType in filesByKey:
						filesByKey[bookType] = open('Output/Button-Maps/Button-Maps/' + keyCodes[bookType] + '.txt', 'w', encoding = 'utf-8')
					
					altTitle = line[7] if not str(line[7]) == 'nan' else ''

					infoLine = '' if line[1] != 'AK' else '\t' + str(line[5])
					filesByKey[bookType].write(line[6] + '\t' + altTitle + '\t' + folderPath + line[9] + infoLine + '\n')

		for file in filesByKey.values():
			file.close()

class LyricsParser():
	def __init__(self):
		file_name =  "Input/input.xlsx"
		df = pd.read_excel(file_name, sheet_name= "COMBINE_PADS")

		if not os.path.isdir('Output/Button-Maps/Button-Maps'):
			os.makedirs('Output/Button-Maps/Button-Maps/')
		outputFile = open('Output/Button-Maps/Button-Maps/csv4.txt', 'w', encoding='utf-8')

		for line in list(df.values)[1:]:
			line = [x.strip() if type(x) == str else x for x in line[1:]]

			if not ('nan' in [str(x) for x in (line[0], line[1], line[2])]):
				k = '\t'.join([str(x).strip() for x in [line[x] for x in [1, 0, 2]]]) 
				k += '\t' + ''.join(['Y' if str(x) != 'nan' else 'N' for x in line[5:13]])
				k +=  '\t' + str(line[15])
			outputFile.write(k + '\n')

		outputFile.close()

class SearchablesParser():
	def __init__(self):
		file_name =  "Input/input.xlsx"
		df = pd.read_excel(file_name, sheet_name= "AkramVignan")

		if not os.path.isdir('Output/Button-Maps/Button-Maps'):
			os.makedirs('Output/Button-Maps/Button-Maps/')
		outputFile = open('Output/Button-Maps/Button-Maps/akram-vignan.txt', 'w', encoding='utf-8')
		for line in list(df.values):
			line = [x.strip() if type(x) == str else x for x in line]

			if not ('nan' in [str(x) for x in (line[0], line[1], line[2])]):
				k = str(line[9]) + '\t' + str(line[6]) + '\t' + str(line[7]) + '\t\t' + str(line[5])
				outputFile.write(k + '\n')

class FirbebaseSync():
	def hash_bytestr_iter(self, bytesiter, hasher, ashexstr=True):
	    for block in bytesiter:
	        hasher.update(block)
	    return (hasher.hexdigest() if ashexstr else hasher.digest())

	def file_as_blockiter(self, afile, blocksize=65536):
	    with afile:
	        block = afile.read(blocksize)
	        while len(block) > 0:
	            yield block
	            block = afile.read(blocksize)

	def generateCheckSums(self):
		#fnamelst = ['aarti-vidhi.zip', 'aptavani.zip', 'books.zip', 'Button-Maps.zip', 'images.zip', 'natukaka.zip', 'swarvadini.zip']
		fnamelst = os.listdir('Output/Zipped') 
		md5sums = [(fname, self.hash_bytestr_iter(self.file_as_blockiter(open('Output/Zipped/' + fname, 'rb')), hashlib.md5())) for fname in fnamelst]
		return md5sums

	def __init__(self):
		cred = credentials.Certificate('Input/credentials.json')
		firebase_admin.initialize_app(cred, {'databaseURL' : 'https://toc-file-sync.firebaseio.com/'})
		dinos = db.reference('deploy-zipped')
		k = {x[0].replace('.','_') : x[1] for x in self.generateCheckSums()}
		dinos.set(dict(k))

class CleanUp():
	def __init__(self):
		shutil.rmtree('Output/Button-Maps/')
		shutil.rmtree('__pycache__/')

#ButtonMapGenerator()
#LyricsParser()
#SearchablesParser()
#shutil.make_archive("Output/Zipped/Button-Maps", 'zip', "Output/Button-Maps/")
FirbebaseSync()
#CleanUp()
