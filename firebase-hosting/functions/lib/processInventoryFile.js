const StreamZip = require('node-stream-zip');
const uuid = require('uuid');
const path = require('path');
const os = require('os');
const fs = require('fs');
const parse = require('csv-parse')

const expected_supplier_inverntory_csv_filename = "supplier_inventory_list.csv"
exports.processSupplierInventoryFiles = function (fileList, bulk_import_id, cb) {
    console.log('Started processing the inventory files list:' + JSON.stringify(fileList));
    var file = fileList['supplier_inventory_file']
    if (file === undefined || (!file.toLowerCase().endsWith('.zip'))) {
        console.log('Input file is not a zip to process it');
        cb(new Error('Failed to process the file as the uploaded input file is not a zip file.'))
        return;
    }
    const tmpdir = os.tmpdir();

    console.log('Processing the input file:' + file);
    const zip = new StreamZip({
        file: file,
        storeEntries: true
    });
    zip.on('error', function (err) {
        console.log('Error occurred while reading the uploaded zip file. Error:' + err)
        zip.close();
        return cb(err, "FAIL");
    });

    zip.on('ready', function () {
        id = uuid.v4();
        extracted_folder = tmpdir + path.sep + id;
        fs.mkdirSync(extracted_folder);
        zip.extract(null, extracted_folder, (err, count) => {
            if (err) {
                console.log('Error while extracting the zip files. Error:' + err)
                return cb(err, "FAIL");
            }
            console.log("Extracted the files into folder:" + extracted_folder + ", No. of entries:" + count);
            zip.close();
            processExtractedFiles(id, cb)
        });
    })
}

isFileExists = function (filepath) {
    console.log('INSIDE FUN::Checking for the file availablility:' + filepath)
    flag = true;
    try {
        fs.accessSync(filepath, fs.constants.F_OK);
        console.log('File is available..');
    } catch (e) {
        console.log("Error while accessing the file. Error:" + e)
        flag = false;
    }
    console.log("The file availability flag is:" + flag)
    return flag;
}

processExtractedFiles = function (id, cb) {
    console.log('Processing the extracted files from folder:' + id);
    const tmpdir = os.tmpdir();
    folderToCheck = tmpdir + path.sep + id;
    inventory_file = folderToCheck + path.sep + expected_supplier_inverntory_csv_filename;
    if (! isFileExists(inventory_file)) {
        console.log('The uploaded zip file doesnt contain the expected csv file to process.');
        return cb(new Error('The uploaded zip file doesnt contain the expected csv file to process.'), "FAIL")
    }
    console.log('File CSV file:' + inventory_file + " is available for processing");
    fs.readFile(inventory_file, function (err, content) {
        if (err) {
            console.log('Error occurred while reading the supplier inventory file uploaded. Error:' + err);
            cb(err, "FAIL")
            return;
        }
        parse(content, {
            comment: '#'
        }, function (err, rows) {
            if (err) {
                console.log('Error occurred while parsing the supplier inventory file uploaded. Error:' + err);
                cb(err, "FAIL")
                return;
            }
            console.log('List of rows:' + JSON.stringify(rows));
            errValue = getErrorsOnInvalidSupplierInventoryFile(rows)
            if (errValue) {
                cb(new Error(errValue), "FAIL")
                return;
            }
            supplier_name = getSupplierName(rows);
            supplier_inventories = getSupplierInventories(rows, folderToCheck);
            cb(undefined, {
                'name' : supplier_name,
                'inventories_list' : supplier_inventories
            })
        })
    })
}

getErrorsOnInvalidSupplierInventoryFile = function (rows) {
    if (rows === undefined || rows.length < 3) {
        console.log('Either the file rows are invalid or minimum number of rows is invalid')
        return "Either the file rows are invalid or minimum number of rows is invalid"
    }
    if (getSupplierName(rows) === undefined
        || getSupplierInventoryHeaders(rows) === undefined) {
        console.log('Either Supplier name or headers found to be invalid from the file')
        return "Either Supplier name or headers found to be invalid from the file"
    }
    return undefined;
}

getSupplierName = function (rows) {
    supplier_row = rows[0]
    console.log('Supplier name row is:' + JSON.stringify(supplier_row))
    if(supplier_row.length < 1) {
        console.log('Supplier name row is found to be invalid.');
        return undefined;
    }
    support_name_col_value = supplier_row[0].trim()
    support_name = supplier_row[1].trim()
    if (support_name_col_value.toLowerCase() === "suppliername") {
        return support_name.trim();
    }
    return undefined;
}

getSupplierInventoryHeaders = function (rows) {
    headers_row = rows[1]
    console.log('Headers row is:' + JSON.stringify(headers_row))
    if (headers_row === undefined || headers_row.length < 5) {
        console.log('Number of columns in the header row is invalid');
        return undefined;
    }
    if (headers_row[0].trim().toLowerCase() === "categoryname"
        && headers_row[1].trim().toLowerCase() === "itemname"
        && headers_row[2].trim().toLowerCase() === "itemprice"
        && headers_row[3].trim().toLowerCase() === "itemquantitytype"
        && headers_row[4].trim().toLowerCase() === "itemimagefile"
    ) {
        return headers_row;
    } else {
        console.log('Error as the invalid values found in the headers')
    }
    return undefined;
}


getSupplierInventories = function(rows, downloadedFolder) {
    var data = {}
    var inventories = []
    var uniqueCategories = []
    for(i = 2; i < rows.length; i++) {
        var rowData = rows[i]
        var item = {}
        item['categoryname'] = rowData[0].trim();
        item['itemname'] = rowData[1].trim();
        item['itemprice'] = rowData[2].trim();
        item['itemquantitytype'] = rowData[3].trim();
        item['itemimagefilename'] = rowData[4].trim();
        if(rowData[4].trim().length > 0) {
            item['itemimagefile'] = downloadedFolder + path.sep + rowData[4].trim();
        } else {
            item['itemimagefile'] = ""
        }
        item["attr1"] = "0";
        item["attr2"] = "0";
        item["attr3"] = "0";
        if(rowData.length >= 6 && rowData[5] !== undefined) {
            item['attr1'] = rowData[5].trim();
        }
        if(rowData.length >= 7 && rowData[6] !== undefined) {
            item['attr2'] = rowData[6].trim();
        }
        if(rowData.length >= 8 &&  rowData[7] !== undefined) {
            item['attr3'] = rowData[7].trim();
        }

        inventories.push(item)
        if(uniqueCategories.indexOf(rowData[0].trim()) === -1) {
            uniqueCategories.push(rowData[0].trim());
        }
    }    
    data['inventories'] = inventories;
    data['categories'] = uniqueCategories;
    return data;
}
