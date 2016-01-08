import raptureAPI
import multipart
import json
import csv

filename = 'test.csv'
date_column = 0
page_size = 100
prefix = '/future/'
partition = 'mel'
repo = 'series'
rapture_url = 'localhost:8665/rapture'
username = 'rapture'
password = 'rapture'
client = raptureAPI.raptureAPI(rapture_url, username, password)

def fix_header(prefix,raw):
  return prefix+(raw.replace(" ", "_"))

def fix_date(date):
  return date

def init_page(headers):
  page = {}
  count = 0
  for header in headers:
    if count != date_column:
      page[header] = []
    count = count + 1
  return page

def send_page(headers, dates, page, client):
  count = 0
  for header in headers:
    if count != date_column:
      client.doSeires_AddDoublesToSeries(parititon, repo, header, dates, page[header])
    count = count + 1

headers = []
count = 0
reader = csv.reader(open(filename, 'rb'))
raw_headers = reader.next()
for raw_header in raw_headers:
  headers.append(fix_header(prefix,raw_header))
  count = count + 1

page = init_page(headers)
dates = []
rowcount = 0
for row in reader:
  colcount = 0
  for val in row:
    if colcount == date_column:
      dates.append(fix_date(val))
    else:
      page[headers[colcount]].append(val)    
    colcount = colcount + 1
  rowcount = rowcount + 1
  if rowcount == page_size:
    send_page(headers, dates, page, client)
    dates = []
    page = init_page(headers)
    rowcount = 0
if rowcount > 0:
  send_page(headers, dates, page, client)
