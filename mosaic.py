#!/usr/bin/env python
import re 
import os
import sys

class Dimension:
    def __init__(self, width, height):
        self.width = width
        self.height = height

def get_sizes():
    input = "sizes.txt"
    f = open(input, "r")
    result = []
    row = []
    for line in f.readlines():
        line = line.strip()
        if line != "*":
            width_height = line.split("x")
            row.append(Dimension(int(width_height[0]), int(width_height[1])))
        else:
            result.append(row)
            row = []

    f.close()
    return result

def scale_factors(by_rows, final_side, border):
    matrix = get_sizes()
    result = []
    for side in matrix:
        side_length = len(side)
        new_side = []
        side_sum = 0
        for cell in side:
            if (by_rows == "rows"):
                new_s = 1000*cell.width/cell.height
            else:
                new_s = 1000*cell.height/cell.width

            new_side.append(new_s)
            side_sum = side_sum + new_s
        
        coeff = float(final_side - border*(side_length+1))/float(side_sum)
        scale_side = []
        
        for index in range(side_length):
            if (by_rows == "rows"):
                cell_coeff = float(new_side[index]*coeff)/float(side[index].width)
            else: 
                cell_coeff = float(new_side[index]*coeff)/float(side[index].height)
            scale_side.append(cell_coeff)
            
        result.append(scale_side)
    
    return result

def write_factors(by_rows, final_side, border):
    output = "scale_factors.txt"
    f = open(output, "w")
    
    matrix = scale_factors(by_rows, final_side, border)
    
    first = 1
    for side in matrix:
        if (first == 0):
            f.write("\n")
        
        if (first == 1):
            first = 0
        
        for cell in side:
            f.write("%0.4f " % (cell*100))  
    
    f.write("\n")
    f.close()

by_rows = sys.argv[1]
final_side = int(sys.argv[2])
border = int(sys.argv[3])

write_factors(by_rows, final_side, border)
