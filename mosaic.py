#!/usr/bin/env python
import re 
import os
import sys

def get_sizes():
    input = "sizes.txt"
    f = open(input, "r")
    result = []
    row = []
    for line in f.readlines():
        line = line.strip()
        if line != "*":
            width_height = line.split("x")
            row.append([int(width_height[0]), int(width_height[1])])
        else:
            result.append(row)
            row = []

    f.close()
    return result

def scale_factors(final_width, border):
    matrix = get_sizes()
    result = []
    for row in matrix:
        row_length = len(row)
        new_width = []
        width_sum = 0
        for cell in row:
            new_w = 1000*cell[0]/cell[1]
            new_width.append(new_w)
            width_sum = width_sum + new_w
        
        coeff = float(final_width - border*(row_length+1))/float(width_sum)
        scale_row = []
        
        for index in range(row_length):
            cell_coeff = float(new_width[index]*coeff)/float(row[index][0])
            scale_row.append(cell_coeff)
            
        result.append(scale_row)
    
    return result

def write_factors(final_width, border):
    output = "scale_factors.txt"
    f = open(output, "w")
    
    matrix = scale_factors(final_width, border)
    
    first = 1
    for row in matrix:
        if (first == 0):
            f.write("\n")
        
        if (first == 1):
            first = 0
        
        for cell in row:
            f.write("%0.4f " % (cell*100))  
    
    f.write("\n")
    f.close()

final_width = int(sys.argv[1])
border = int(sys.argv[2])

write_factors(final_width, border)