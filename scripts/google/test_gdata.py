#!/usr/bin/python

import time
import gdata.spreadsheet.service
import getpass
import string
from experiments.core.google_spreadsheets import get_spreadsheet_by_title,\
    get_first_worksheet, CellsGetAction, ListInsertAction, clear_worksheet,\
    ListGetAction

if __name__ == "__main__":
    
    email = 'matthew.gormley@gmail.com'
    print "Enter password for",email
    password = getpass.getpass()
    
    gd_client = gdata.spreadsheet.service.SpreadsheetsService()
    gd_client.email = email
    gd_client.password = password
    gd_client.source = 'exampleCo-exampleApp-1'
    gd_client.ProgrammaticLogin()
    
    #key = PromptForSpreadsheet(gd_client)
    #wksht_id = PromptForWorksheet(gd_client, key)
    key = get_spreadsheet_by_title(gd_client, "Temporary Results")
    print "Spreadsheet key:",key
    wksht_id = get_first_worksheet(gd_client, key)
    print "Worksheet id:",wksht_id
    feed = ListGetAction(gd_client, key, wksht_id)
    
    #ListInsertAction(gd_client, key, wksht_id, {"header1":"asdf", "header3":"qwer"})
    #clear_worksheet(gd_client, key, wksht_id)
    
# # Prepare the dictionary to write
# dict = {}
# dict['date'] = time.strftime('%m/%d/%Y')
# dict['time'] = time.strftime('%H:%M:%S')
# dict['weight'] = weight
# print dict

# entry = spr_client.InsertRow(dict, spreadsheet_key, worksheet_id)
# if isinstance(entry, gdata.spreadsheet.SpreadsheetsList):
#   print "Insert row succeeded."
# else:
#   print "Insert row failed."
