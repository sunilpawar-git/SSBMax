#!/bin/bash
# skills.sh - Helper script to manage and inspect Agent Skills for SSBMax
# Agent Skills are modular procedural knowledge packages located in .agent/skills/
# Last Updated: January 25, 2026

set -e

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0m'
CYAN='\033[0;36m'
NC='\033[0m'

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
SKILLS_DIR="$PROJECT_ROOT/.agent/skills"

function show_help() {
    echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║          SSBMax Agent Skills Manager                       ║${NC}"
    echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo "Usage: ./skills.sh [command] [arguments]"
    echo ""
    echo "Commands:"
    echo -e "  ${GREEN}list${NC}                List all installed agent skills"
    echo -e "  ${GREEN}inspect${NC} [name]      Show detailed instructions for a skill"
    echo -e "  ${GREEN}verify${NC}              Check if all skills have valid SKILL.md files"
    echo -e "  ${GREEN}create${NC} [name]       Create a new skill template"
    echo -e "  ${GREEN}help${NC}                Show this help message"
    echo ""
    echo "Available Skills:"
    echo -e "  ${CYAN}create-test-module${NC}   - Create a new SSB test module"
    echo -e "  ${CYAN}add-string-resource${NC}  - Add string resources correctly"
    echo -e "  ${CYAN}create-viewmodel${NC}     - Create a ViewModel with proper patterns"
    echo ""
    echo "Example:"
    echo "  ./skills.sh inspect create-test-module"
}

function list_skills() {
    echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║          Installed Agent Skills                            ║${NC}"
    echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    
    if [ ! -d "$SKILLS_DIR" ]; then
        echo -e "${YELLOW}⚠ No skills directory found at $SKILLS_DIR${NC}"
        echo -e "  Run: mkdir -p $SKILLS_DIR"
        return
    fi

    local count=0
    for skill in "$SKILLS_DIR"/*; do
        if [ -d "$skill" ]; then
            name=$(basename "$skill")
            if [ -f "$skill/SKILL.md" ]; then
                description=$(grep "^description:" "$skill/SKILL.md" 2>/dev/null | head -n 1 | sed 's/description: *"//' | sed 's/"$//' || echo "No description")
                echo -e "  ${GREEN}✓${NC} ${CYAN}$name${NC}"
                echo -e "    $description"
                echo ""
                count=$((count+1))
            fi
        fi
    done
    
    if [ $count -eq 0 ]; then
        echo -e "${YELLOW}  No skills installed yet.${NC}"
    else
        echo -e "${GREEN}Total: $count skill(s) installed${NC}"
    fi
}

function inspect_skill() {
    local skill_name=$1
    if [ -z "$skill_name" ]; then
        echo -e "${RED}Error: Skill name required.${NC}"
        echo "Usage: ./skills.sh inspect <skill_name>"
        echo ""
        echo "Available skills:"
        list_skill_names
        exit 1
    fi

    local skill_file="$SKILLS_DIR/$skill_name/SKILL.md"
    if [ -f "$skill_file" ]; then
        echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
        echo -e "${BLUE}║  Skill: $skill_name${NC}"
        echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}"
        echo ""
        cat "$skill_file"
    else
        echo -e "${RED}Error: Skill '$skill_name' not found.${NC}"
        echo ""
        echo "Available skills:"
        list_skill_names
        exit 1
    fi
}

function list_skill_names() {
    for skill in "$SKILLS_DIR"/*; do
        if [ -d "$skill" ] && [ -f "$skill/SKILL.md" ]; then
            echo -e "  ${CYAN}$(basename "$skill")${NC}"
        fi
    done
}

function verify_skills() {
    echo -e "${BLUE}Verifying Agent Skills...${NC}"
    echo ""
    
    if [ ! -d "$SKILLS_DIR" ]; then
        echo -e "${RED}✗ Skills directory not found: $SKILLS_DIR${NC}"
        exit 1
    fi
    
    local errors=0
    local passed=0
    
    for skill in "$SKILLS_DIR"/*; do
        if [ -d "$skill" ]; then
            local name=$(basename "$skill")
            if [ ! -f "$skill/SKILL.md" ]; then
                echo -e "${RED}  ✗ $name${NC} - missing SKILL.md"
                errors=$((errors+1))
            else
                echo -e "${GREEN}  ✓ $name${NC}"
                passed=$((passed+1))
            fi
        fi
    done

    echo ""
    if [ $errors -eq 0 ]; then
        echo -e "${GREEN}════════════════════════════════════════${NC}"
        echo -e "${GREEN}✓ All $passed skill(s) verified successfully!${NC}"
        echo -e "${GREEN}════════════════════════════════════════${NC}"
    else
        echo -e "${RED}════════════════════════════════════════${NC}"
        echo -e "${RED}✗ Verification failed: $errors error(s), $passed passed${NC}"
        echo -e "${RED}════════════════════════════════════════${NC}"
        exit 1
    fi
}

function create_skill() {
    local skill_name=$1
    if [ -z "$skill_name" ]; then
        echo -e "${RED}Error: Skill name required.${NC}"
        echo "Usage: ./skills.sh create <skill_name>"
        exit 1
    fi
    
    local skill_dir="$SKILLS_DIR/$skill_name"
    
    if [ -d "$skill_dir" ]; then
        echo -e "${RED}Error: Skill '$skill_name' already exists.${NC}"
        exit 1
    fi
    
    mkdir -p "$skill_dir"
    
    cat > "$skill_dir/SKILL.md" << 'EOFTEMPLATE'
# Skill Name

description: "Brief description of what this skill does"

## Overview

Describe the purpose and use case for this skill.

## Prerequisites

List any prerequisites or setup required.

## Step-by-Step Process

### Step 1: First Step

Detailed instructions...

### Step 2: Second Step

More instructions...

## Reference Files

- `path/to/reference/file.kt`

## Checklist

- [ ] Item 1
- [ ] Item 2
EOFTEMPLATE

    echo -e "${GREEN}✓ Created skill template: $skill_dir/SKILL.md${NC}"
    echo "  Edit the SKILL.md file to add your skill instructions."
}

case "$1" in
    list)
        list_skills
        ;;
    inspect)
        inspect_skill "$2"
        ;;
    verify)
        verify_skills
        ;;
    create)
        create_skill "$2"
        ;;
    help|--help|-h)
        show_help
        ;;
    *)
        show_help
        ;;
esac
