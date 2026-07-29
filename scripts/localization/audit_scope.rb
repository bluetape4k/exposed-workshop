#!/usr/bin/env ruby
# frozen_string_literal: true

require "find"
require "optparse"
require "open3"
require "set"

ROOT = File.expand_path("../..", __dir__)

EXCLUDED_DIRS = Set.new(%w[
  .git
  .gradle
  .idea
  .kotlin
  .omc
  .omx
  .code-review-graph
  .codegraph
  .worktrees
  build
])

LLM_FACING_DOCS = Set.new(%w[
  AGENTS.md
  CLAUDE.md
  .github/copilot-instructions.md
])

EXPECTED = {
  markdown_total: 279,
  llm_facing_docs: 3,
  readme_pair_dirs: 86,
  readme_files: 172,
  single_language_docs: 104,
  ko_named_single_language_docs: 0,
  kotlin_files: 912,
  comment_bearing_kotlin_files: 604,
  kdoc_files: 507,
  kdoc_tag_files: 102,
  commentish_lines: 23_959,
}.freeze

options = {
  base: nil,
  check_diff: false,
  strict_counts: false,
  list: false,
}

OptionParser.new do |parser|
  parser.banner = "Usage: ruby scripts/localization/audit_scope.rb [options]"
  parser.on("--base REF", "Git ref used when --check-diff is enabled") { |value| options[:base] = value }
  parser.on("--check-diff", "Fail when changed paths touch primary-scope exclusions") { options[:check_diff] = true }
  parser.on("--strict-counts", "Fail when inventory counts drift from the approved baseline") { options[:strict_counts] = true }
  parser.on("--list", "Print candidate path lists") { options[:list] = true }
end.parse!

def relative_files
  files = []
  Find.find(ROOT) do |path|
    name = File.basename(path)
    if File.directory?(path)
      Find.prune if EXCLUDED_DIRS.include?(name)
      next
    end
    files << path.delete_prefix("#{ROOT}/")
  end
  files
end

def markdown_file?(path)
  path.end_with?(".md", ".mdx")
end

def kotlin_file?(path)
  path.end_with?(".kt", ".kts")
end

def readme_file?(path)
  File.basename(path).match?(/\AREADME(?:\.ko)?\.md\z/)
end

def manual_file?(path)
  path.start_with?("docs/manual/en/") || path.start_with?("docs/manual/ko/")
end

def readme_pairs(readmes)
  by_dir = readmes.group_by { |path| File.dirname(path) }
  paired_dirs = by_dir.keys.select do |dir|
    by_dir.fetch(dir).include?(readme_path(dir, "README.md")) &&
      by_dir.fetch(dir).include?(readme_path(dir, "README.ko.md"))
  end
  paired_files = paired_dirs.flat_map do |dir|
    [readme_path(dir, "README.md"), readme_path(dir, "README.ko.md")]
  end
  [paired_dirs, paired_files]
end

def readme_path(dir, name)
  dir == "." ? name : File.join(dir, name)
end

def manual_relative_set(prefix, files)
  files
    .select { |path| path.start_with?(prefix) }
    .map { |path| path.delete_prefix(prefix) }
    .to_set
end

def comment_metrics(files)
  metrics = {
    comment_bearing_kotlin_files: 0,
    kdoc_files: 0,
    kdoc_tag_files: 0,
    commentish_lines: 0,
  }

  files.each do |path|
    text = File.read(File.join(ROOT, path), invalid: :replace, undef: :replace)
    has_comment = text.match?(%r{//|/\*|\*/|\*\s|@(?:param|property|return|throws|receiver)\b})
    next unless has_comment

    metrics[:comment_bearing_kotlin_files] += 1
    metrics[:kdoc_files] += 1 if text.include?("/**")
    metrics[:kdoc_tag_files] += 1 if text.match?(/@(param|property|return|throws|receiver)\b/)
    text.each_line do |line|
      metrics[:commentish_lines] += 1 if line.match?(%r{\A\s*(//|/\*|\*|\*/)|@(param|property|return|throws|receiver)\b})
    end
  end

  metrics
end

def git_changed_paths(base)
  stdout, stderr, status = Open3.capture3("git", "-C", ROOT, "diff", "--name-only", "#{base}...HEAD")
  abort "git diff failed for #{base}...HEAD: #{stderr}" unless status.success?

  stdout.lines.map(&:chomp).reject(&:empty?)
end

def fail_with(messages)
  return if messages.empty?

  warn "Localization scope audit failed:"
  messages.each { |message| warn "- #{message}" }
  exit 1
end

files = relative_files
markdown = files.select { |path| markdown_file?(path) }
kotlin = files.select { |path| kotlin_file?(path) }
llm_facing = markdown.select { |path| LLM_FACING_DOCS.include?(path) }
manual = markdown.select { |path| manual_file?(path) }
readmes = markdown.select { |path| readme_file?(path) }
paired_readme_dirs, paired_readme_files = readme_pairs(readmes)

single_language_docs = (markdown - llm_facing - manual - readmes).reject { |path| path.end_with?(".ko.md") }
ko_named_single_language_docs = (markdown - llm_facing - manual - readmes).select { |path| path.end_with?(".ko.md") }
comment_counts = comment_metrics(kotlin)

inventory = {
  markdown_total: markdown.size,
  llm_facing_docs: llm_facing.size,
  readme_pair_dirs: paired_readme_dirs.size,
  readme_files: readmes.size,
  single_language_docs: single_language_docs.size,
  ko_named_single_language_docs: ko_named_single_language_docs.size,
  kotlin_files: kotlin.size,
}.merge(comment_counts)

failures = []
EXPECTED.each do |key, expected|
  actual = inventory.fetch(key)
  failures << "#{key}: expected #{expected}, got #{actual}" if options[:strict_counts] && actual != expected
end

unpaired_readmes = readmes - paired_readme_files
failures << "unpaired README files: #{unpaired_readmes.join(', ')}" unless unpaired_readmes.empty?

manual_en = manual_relative_set("docs/manual/en/", manual)
manual_ko = manual_relative_set("docs/manual/ko/", manual)
missing_ko = manual_en - manual_ko
missing_en = manual_ko - manual_en
failures << "manual paths missing Korean pair: #{missing_ko.to_a.sort.join(', ')}" unless missing_ko.empty?
failures << "manual paths missing English pair: #{missing_en.to_a.sort.join(', ')}" unless missing_en.empty?

if options[:check_diff]
  abort "--base is required with --check-diff" if options[:base].nil? || options[:base].empty?

  changed_paths = git_changed_paths(options[:base])
  excluded_changes = changed_paths.select do |path|
    LLM_FACING_DOCS.include?(path) ||
      readme_file?(path) ||
      manual_file?(path)
  end
  failures << "changed primary-scope exclusion paths: #{excluded_changes.join(', ')}" unless excluded_changes.empty?
end

fail_with(failures)

puts "Localization scope inventory"
inventory.each { |key, value| puts "#{key}=#{value}" }
puts "manual_en_files=#{manual_en.size}"
puts "manual_ko_files=#{manual_ko.size}"

if options[:list]
  puts
  puts "single_language_docs:"
  single_language_docs.sort.each { |path| puts path }
end
