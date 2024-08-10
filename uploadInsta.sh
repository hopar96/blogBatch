#!/bin/bash

FILES="${1}"
echo "FILES : "$FILES

VENV_DIR="/Users/hojun/study/algorithm_problem_solve/venv"

source $VENV_DIR/bin/activate

python3 /Users/hojun/study/algorithm_problem_solve/uploadInsta.py "${FILES}"	

deactivate
