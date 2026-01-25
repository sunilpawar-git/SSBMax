#!/bin/bash
# skills.sh - Helper script to manage and inspect Agent Skills for SSBMax
# Agent Skills are modular procedural knowledge packages located in .agent/skills/

set -e

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
SKILLS_DIR="$PROJECT_ROOT/.agent/skills"

function show_help() {
    echo -e "${BLUE}SSBMax Agent Skills Manager${NC}"
    echo "Usage: ./skills.sh [command]"
    echo ""
    echo "Commands:"
    echo "  list      List all installed agent skills"
    echo "  inspect   [skill_name] Show detailed instructions for a skill"
    echo "  verify    Check if all skills have valid SKILL.md files"
    echo "  help      Show this help message"
}

function list_skills() {
    echo -e "${BLUE}Installed Agent Skills:${NC}"
    if [ ! -d "$SKILLS_DIR" ]; then
        echo -e "${YELLOW}No skills directory found at $SKILLS_DIR${NC}"
        return
    fi

    for skill in "$SKILLS_DIR"/*; do
        if [ -d "$skill" ]; then
            name=$(basename "$skill")
            description=$(grep "description:" "$skill/SKILL.md" | head -n 1 | cut -d'"' -f2)
            echo -e "  ${GREEN}• $name${NC} - $description"
        fi
    done
}

function inspect_skill() {
    local skill_name=$1
    if [ -z "$skill_name" ]; then
        echo -e "${RED}Error: Skill name required.${NC}"
        exit 1
    fi

    local skill_file="$SKILLS_DIR/$skill_name/SKILL.md"
    if [ -f "$skill_file" ]; then
        echo -e "${BLUE}--- Skill: $skill_name ---${NC}"
        cat "$skill_file"
    else
        echo -e "${RED}Error: Skill '$skill_name' not found.${NC}"
        exit 1
    fi
}

function verify_skills() {
    echo -e "${BLUE}Verifying Agent Skills...${NC}"
    local errors=0
    for skill in "$SKILLS_DIR"/*; do
        if [ -d "$skill" ]; then
            if [ ! -f "$skill/SKILL.md" ]; then
                echo -e "${RED}  [FAIL]${NC} $(basename "$skill") is missing SKILL.md"
                errors=$((errors+1))
            else
                echo -e "${GREEN}  [OK]${NC} $(basename "$skill")"
            fi
        fi
    done

    if [ $errors -eq 0 ]; then
        echo -e "\n${GREEN}All skills verified successfully!${NC}"
    else
        echo -e "\n${RED}Verification failed with $errors error(s).${NC}"
        exit 1
    fi
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
    help|*)
        show_help
        ;;
esac
