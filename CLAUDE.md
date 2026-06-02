# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a **classroom availability viewer** (空闲教室查看系统) for 博学楼 (Boxue Building). The system shows which classrooms are currently free or occupied based on Excel schedule data.

## Architecture

- **Frontend**: Vue 3 — large screen UI displaying classroom grid, color-coded by availability (green = free, red = occupied)
- **Backend**: Spring Boot + MyBatis + MySQL
- **Data Source**: Excel (.xls) files in the repo root. Each filename is the classroom name (e.g., `博学楼102.xls`). Labs/special rooms have descriptive prefixes (e.g., `雕塑实验室-博学楼-003.xls`).

## Key Files

- `需求.md` — project requirements document
- `*.xls` — classroom schedule Excel files (~110 rooms across floors 1-6 plus basement A/B and floor 7 labs)

## Data Conventions

- Room numbering: `博学楼{floor}{room}` (e.g., `博学楼301` = floor 3, room 01)
- Floors 1-6 use 3-digit codes; basement rooms use letter prefixes (A, B)
- Room 108, 208, 308, 408, 508, 608 do not exist (skipped in numbering)
- Special labs use format: `{lab description}-博学楼-{room number}.xls`

## Development

The project is in early planning stage — no code has been written yet. When implementing:

- Parse Excel files at startup or via a data import step to populate MySQL
- Frontend should poll or receive real-time data comparing current time against schedule slots
